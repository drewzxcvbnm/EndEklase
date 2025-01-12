package lv.app.backend.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lv.app.backend.dto.InvoiceCreateDTO;
import lv.app.backend.dto.InvoiceDTO;
import lv.app.backend.mappers.EntityMapper;
import lv.app.backend.model.Attendance;
import lv.app.backend.model.Child;
import lv.app.backend.model.Invoice;
import lv.app.backend.model.User;
import lv.app.backend.model.repository.AttendanceRepository;
import lv.app.backend.model.repository.InvoiceRepository;
import lv.app.backend.model.repository.LessonRepository;
import lv.app.backend.model.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;

import static lv.app.backend.util.Common.flatten;

@Slf4j
@Service
@RequiredArgsConstructor
public class InvoiceService {

    private final AttendanceRepository attendanceRepository;
    @Value("${lesson-attendance-cost}")
    private Long cost;

    private final EntityMapper entityMapper;
    private final UserRepository userRepository;
    private final LessonRepository lessonRepository;
    private final InvoiceRepository invoiceRepository;

    @Transactional
    public void updateInvoice(InvoiceDTO dto) {
        Invoice invoice = invoiceRepository.findById(dto.getId())
                .orElseThrow(() -> new RuntimeException("Invoice not found"));
        entityMapper.updateInvoice(invoice, dto);
        if (dto.getUserId() != null) {
            invoice.setUser(userRepository.getReferenceById(dto.getUserId()));
        }
        invoiceRepository.save(invoice);
    }

    @Transactional
    public void createInvoices(LocalDate startDate, LocalDate endDate) {
        List<User> users = userRepository.findAll();
        users.forEach(u -> {
            List<Long> lessons = lessonRepository.findUserLessonsToPay(startDate, endDate, u);
            if (lessons.isEmpty()) {
                log.error("No lessons to pay for user {}", u);
                return;
            }
            this.createInvoice(InvoiceCreateDTO.builder()
                    .userId(u.getId())
                    .lessonIds(lessons)
                    .build());
        });
    }

    @Transactional
    public void createInvoice(InvoiceCreateDTO dto) {
        User user = userRepository.findById(dto.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found: " + dto.getUserId()));
        if (dto.getAmount() != null) {
            createManualInvoice(dto, user);
            return;
        }
        if (!user.isSeparateInvoices()) {
            makeSingleInvoice(dto, user);
            return;
        }
        makeInvoicesForEachChild(dto, user);
    }

    @Transactional
    public List<InvoiceDTO> getAllInvoices() {
        return invoiceRepository.findAll().stream()
                .map(entityMapper::invoiceToDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public List<InvoiceDTO> getInvoicesByUser(Long userId) {
        List<Invoice> invoices = invoiceRepository.findByUserId(userId);
        return invoices.stream()
                .map(entityMapper::invoiceToDto)
                .collect(Collectors.toList());
    }

    private void makeInvoicesForEachChild(InvoiceCreateDTO dto, User user) {
        Function<Child, Double> costRateGenerator = getCostRateGenerator(user);
        user.getChildren().forEach(c -> {
            double multiChildDiscount = costRateGenerator.apply(c);
            List<Attendance> attendancesToPay = getAttendancesToPay(c, dto.getLessonIds());
            setAttendanceCost(attendancesToPay, multiChildDiscount, user);
            formInvoice(c.getParent(), attendancesToPay);
        });
    }

    private void createManualInvoice(InvoiceCreateDTO dto, User user) {
        Invoice invoice = entityMapper.dtoToInvoice(dto);
        invoice.setUser(user);
        List<Attendance> coveredAttendances = dto.getLessonIds().stream()
                .map(lessonRepository::getReferenceById)
                .flatMap(l -> l.getAttendances().stream())
                .filter(a -> user.getChildren().contains(a.getChild()) && a.isAttended() && a.getInvoice() == null)
                .toList();
        coveredAttendances.forEach(a -> a.setInvoice(invoice));
        invoice.setAttendances(coveredAttendances);
        invoiceRepository.saveAndFlush(invoice);
        log.trace("Created manual invoice: {}", invoice);
    }

    private void makeSingleInvoice(InvoiceCreateDTO dto, User user) {
        Function<Child, Double> costRateGenerator = getCostRateGenerator(user);
        List<Pair<Child, List<Attendance>>> attendancesToPay = user.getChildren().stream()
                .map(c -> Pair.of(c, getAttendancesToPay(c, dto.getLessonIds())))
                .toList();
        attendancesToPay.forEach(p -> setAttendanceCost(p.getSecond(), costRateGenerator.apply(p.getFirst()), user));
        formInvoice(user, flatten(attendancesToPay.stream().map(Pair::getSecond).toList()));
    }

    private void setAttendanceCost(List<Attendance> ats, double multiChildDiscount, User user) {
        double discountRate = getDiscountRate(user);
        ats.forEach(a -> a.setCost(Math.round(cost * multiChildDiscount * discountRate)));
    }

    private double getDiscountRate(User user) {
        if (user.getDiscountRate() == null) {
            return 1;
        }
        return (100 - user.getDiscountRate()) / 100;
    }

    private List<Attendance> getAttendancesToPay(Child child, List<Long> lessonsToPay) {
        return child.getAttendances().stream()
                .filter(a -> a.isAttended() && lessonsToPay.contains(a.getLesson().getId()))
                .toList();
    }

    private void formInvoice(User user, List<Attendance> attendancesToPay) {
        if (attendancesToPay.isEmpty()) {
            log.trace("No attendances to pay for user {}", user);
            return;
        }
        long invoiceAmount = attendancesToPay.stream()
                .mapToLong(Attendance::getCost)
                .sum();
        LocalDate currentDate = LocalDate.now();
        Invoice savedInvoice = invoiceRepository.save(Invoice.builder()
                .attendances(attendancesToPay)
                .amount(invoiceAmount)
                .dateIssued(currentDate)
                .dueDate(currentDate.plusWeeks(2))
                .user(user)
                .build());
        log.trace("Created invoice: {}", savedInvoice);
        attendancesToPay.forEach(a -> {
            a.setInvoice(savedInvoice);
            attendanceRepository.save(a);
        });
    }

    private Function<Child, Double> getCostRateGenerator(User user) {
        if (user.getChildren().size() <= 1) {
            return c -> 1.;
        }
        AtomicInteger call = new AtomicInteger();
        Set<Child> children = new HashSet<>();
        return c -> {
            if (children.contains(c)) {
                throw new RuntimeException("Duplicate child given");
            }
            children.add(c);
            int callNum = call.incrementAndGet();
            if (callNum == 1) return 1.; // First child - full rate
            if (callNum == 2) return 0.5; // Second child - 0.5 rate
            if (callNum == 3) return 0.5; // third child - 0.5 rate
            return 1.; // Other children full rate
        };
    }

}
