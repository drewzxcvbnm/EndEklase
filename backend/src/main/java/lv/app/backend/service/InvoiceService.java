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
import java.util.*;
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
    public List<InvoiceDTO> createInvoices(LocalDate startDate, LocalDate endDate) {
        List<User> users = userRepository.findAll();
        return users.stream()
                .flatMap(user -> createInvoiceForUser(startDate, endDate, user).stream())
                .filter(Objects::nonNull)
                .map(entityMapper::invoiceToDto)
                .toList();
    }

    private List<Invoice> createInvoiceForUser(LocalDate startDate, LocalDate endDate, User user) {
        List<Long> lessons = lessonRepository.findUserLessonsToPay(startDate, endDate, user);
        if (lessons.isEmpty()) {
            log.error("No lessons to pay for user {}", user);
            return Collections.emptyList();
        }
        return createInvoice(InvoiceCreateDTO.builder()
                .userId(user.getId())
                .lessonIds(lessons)
                .build());
    }

    @Transactional
    public List<Invoice> createInvoice(InvoiceCreateDTO dto) {
        User user = userRepository.findById(dto.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found: " + dto.getUserId()));

        if (dto.getAmount() != null) {
            return Collections.singletonList(createManualInvoice(dto, user));
        }
        if (!user.isSeparateInvoices()) {
            return Collections.singletonList(makeSingleInvoice(dto, user));
        }
        return makeInvoicesForEachChild(dto, user);
    }

    @Transactional
    public void deleteInvoice(Long invoiceId) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new RuntimeException("Invoice not found with id: " + invoiceId));
        invoiceRepository.delete(invoice);
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

    private List<Invoice> makeInvoicesForEachChild(InvoiceCreateDTO dto, User user) {
        Function<Child, Double> costRateGenerator = getCostRateGenerator(user);
        return user.getChildren().stream().map(c -> {
            double multiChildDiscount = costRateGenerator.apply(c);
            List<Attendance> attendancesToPay = getAttendancesToPay(c, dto.getLessonIds());
            setAttendanceCost(attendancesToPay, multiChildDiscount, user);
            return formInvoice(c.getParent(), attendancesToPay);
        }).toList();
    }

    private Invoice createManualInvoice(InvoiceCreateDTO dto, User user) {
        Invoice invoice = entityMapper.dtoToInvoice(dto);
        invoice.setUser(user);
        List<Attendance> coveredAttendances = dto.getLessonIds().stream()
                .map(lessonRepository::getReferenceById)
                .flatMap(l -> l.getAttendances().stream())
                .filter(a -> user.getChildren().contains(a.getChild()) && a.isAttended() && a.getInvoice() == null)
                .toList();

        coveredAttendances.forEach(a -> a.setInvoice(invoice));
        Invoice savedInvoice = invoiceRepository.saveAndFlush(invoice);

        coveredAttendances.forEach(a -> {
            a.setInvoice(savedInvoice);
            attendanceRepository.save(a);
        });

        log.trace("Created manual invoice with ID {}: {}", savedInvoice.getId(), savedInvoice);
        return savedInvoice;
    }

    private Invoice makeSingleInvoice(InvoiceCreateDTO dto, User user) {
        Function<Child, Double> costRateGenerator = getCostRateGenerator(user);
        List<Pair<Child, List<Attendance>>> attendancesToPay = user.getChildren().stream()
                .map(c -> Pair.of(c, getAttendancesToPay(c, dto.getLessonIds())))
                .toList();

        attendancesToPay.forEach(p -> setAttendanceCost(p.getSecond(), costRateGenerator.apply(p.getFirst()), user));
        return formInvoice(user, flatten(attendancesToPay.stream().map(Pair::getSecond).toList()));
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

    private Invoice formInvoice(User user, List<Attendance> attendancesToPay) {
        if (attendancesToPay.isEmpty()) {
            log.trace("No attendances to pay for user {}", user);
            return null;
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

        return savedInvoice;
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
