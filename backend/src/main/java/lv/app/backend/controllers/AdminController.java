package lv.app.backend.controllers;

import lombok.RequiredArgsConstructor;
import lv.app.backend.dto.*;
import lv.app.backend.mappers.EntityMapper;
import lv.app.backend.mappers.UserMapper;
import lv.app.backend.model.Child;
import lv.app.backend.model.repository.UserRepository;
import lv.app.backend.service.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayInputStream;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Controller for admin exclusive endpoints
 */
@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

    private final UserMapper userMapper;
    private final UserService userService;
    private final EntityMapper entityMapper;
    private final ChildService childService;
    private final LessonService lessonService;
    private final UserRepository userRepository;
    private final InvoiceService invoiceService;
    private final PDFInvoiceGenerator pdfInvoiceGenerator;
    private final KindergartenService kindergartenService;
    private static final Logger logger = LoggerFactory.getLogger(AdminController.class);

    @ResponseBody
    @GetMapping("/user/{userId}")
    public UserDTO getUser(@PathVariable Long userId) {
        return userMapper.userToDto(userRepository.getReferenceById(userId));
    }

    @ResponseBody
    @PostMapping("/user")
    public Map<String, Long> saveUser(@RequestBody UserDTO userDTO) {
        if (userDTO.getId() != null && userDTO.getId() > 0) {
            return Map.of("id", userService.updateUser(userDTO).getId());
        }
        return Map.of("id", userService.createUser(userDTO).getId());
    }

    @GetMapping("/kindergartens")
    public ResponseEntity<List<KindergartenDTO>> getAllKindergartens() {
        List<KindergartenDTO> kindergartens = kindergartenService.getAllKindergartens();
        return ResponseEntity.ok(kindergartens);
    }

    @GetMapping("/kindergartens/{kindergartenId}/groups")
    public ResponseEntity<List<GroupDTO>> getGroupsByKindergarten(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authHeader,
            @PathVariable Long kindergartenId) {
        final String jwt = authHeader.substring(7);
        List<GroupDTO> groups = kindergartenService.getGroupsByKindergarten(kindergartenId);
        return ResponseEntity.ok(groups);
    }

    @GetMapping("/groups/{groupId}/children")
    public ResponseEntity<List<ChildDTO>> getChildrenByGroup(@PathVariable Long groupId) {
        List<Child> children = childService.getChildrenByGroup(groupId);
        List<ChildDTO> childDTOs = children.stream()
                .map(entityMapper::childToDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(childDTOs);
    }

    @GetMapping("/groups/{groupId}/lessons")
    public ResponseEntity<List<LessonDTO>> getLessonsByGroup(@PathVariable Long groupId) {
        List<LessonDTO> lessons = lessonService.getLessonsByGroup(groupId);
        return ResponseEntity.ok(lessons);
    }

    @PostMapping("/children")
    public ResponseEntity<Void> addChild(@RequestBody ChildDTO childDTO) {
        childService.saveChild(childDTO);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/children")
    public ResponseEntity<Void> deleteChildren(@RequestBody List<Long> childIds) {
        childService.deleteChildren(childIds);
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/children")
    public ResponseEntity<Void> addChild(@RequestBody List<ChildDTO> children) {
        childService.updateChildren(children);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/lessons")
    public ResponseEntity<Void> addLesson(@RequestBody LessonDTO lessonDTO) {
        lessonService.saveLesson(lessonDTO);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/lessons")
    public ResponseEntity<List<LessonDTO>> getAllLessons() {
        List<LessonDTO> lessons = lessonService.getAllLessons();
        return ResponseEntity.ok(lessons);
    }

    @GetMapping("/user/{userId}/lessons")
    public ResponseEntity<List<LessonDTO>> getLessonsByUser(@PathVariable Long userId) {
        List<LessonDTO> lessons = lessonService.getLessonsByUser(userId);
        return ResponseEntity.ok(lessons);
    }

    @PostMapping("/invoices")
    public ResponseEntity<List<InvoiceDTO>> createInvoices(@RequestBody Map<String, LocalDate> request) {
        LocalDate startDate = request.get("startDate");
        LocalDate endDate = request.get("endDate");
        List<InvoiceDTO> createdInvoices = invoiceService.createInvoices(startDate, endDate);
        return ResponseEntity.ok(createdInvoices);
    }

    @PostMapping("/invoice")
    public ResponseEntity<List<InvoiceDTO>> createInvoice(@RequestBody InvoiceCreateDTO dto) {
        List<InvoiceDTO> invoices = invoiceService.createInvoice(dto).stream()
                .map(entityMapper::invoiceToDto)
                .toList();
        return ResponseEntity.ok(invoices);
    }

    @PutMapping("/invoice")
    public ResponseEntity<Void> updateInvoice(@RequestBody InvoiceDTO dto) {
        invoiceService.updateInvoice(dto);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/invoices")
    public ResponseEntity<List<InvoiceDTO>> getAllInvoices() {
        List<InvoiceDTO> invoices = invoiceService.getAllInvoices();
        return ResponseEntity.ok(invoices);
    }

    @GetMapping("/invoice/{invoiceId}")
    public ResponseEntity<InvoiceDTO> getInvoiceById(@PathVariable Long invoiceId) {
        return ResponseEntity.ok(invoiceService.getInvoiceById(invoiceId));
    }

    @PutMapping("/attendances")
    public ResponseEntity<Void> updateAttendance(@RequestBody AttendanceDTO attendanceDTO) {
        System.out.println("Received request to update attendance: " + attendanceDTO);
        lessonService.updateAttendanceStatus(attendanceDTO.getChildId(), attendanceDTO.getLessonId(), attendanceDTO.isAttended());
        System.out.println("Attendance updated successfully");
        return ResponseEntity.ok().build();
    }

    @GetMapping("/groups/{groupId}/attendances")
    public ResponseEntity<List<AttendanceDTO>> getAttendanceByGroup(@PathVariable Long groupId) {
        List<AttendanceDTO> attendances = lessonService.getAttendanceByGroup(groupId);
        return ResponseEntity.ok(attendances);
    }

    @GetMapping("/groups/{groupId}/attendances/{yearMonth}")
    public ResponseEntity<List<AttendanceDTO>> getAttendanceByGroupAndMonth(@PathVariable Long groupId, @PathVariable String yearMonth) {
        YearMonth month = YearMonth.parse(yearMonth);
        List<AttendanceDTO> attendances = lessonService.getAttendanceByGroupAndMonth(groupId, month);
        return ResponseEntity.ok(attendances);
    }

    @ResponseBody
    @GetMapping("/user-emails")
    public List<Records.UserInfo> getUserEmails() {
        return userRepository.findAll().stream()
                .map(u -> new Records.UserInfo(u.getId(), u.getFullName()))
                .toList();
    }

    @GetMapping("/invoice/{invoiceId}/pdf")
    public ResponseEntity<InputStreamResource> downloadInvoice(@PathVariable Long invoiceId) {
        ByteArrayInputStream invoice = pdfInvoiceGenerator.generateInvoiceAdmin(invoiceId);
        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Disposition", "attachment; filename=invoice.pdf");
        return ResponseEntity.ok()
                .headers(headers)
                .contentType(MediaType.APPLICATION_PDF)
                .body(new InputStreamResource(invoice));
    }

    @DeleteMapping("/invoice/{invoiceId}")
    public ResponseEntity<Void> deleteInvoice(@PathVariable Long invoiceId) {
        invoiceService.deleteInvoice(invoiceId);
        return ResponseEntity.ok().build();
    }


//    @GetMapping("users/{userId}/invoices")
//    public ResponseEntity<List<InvoiceDTO>> getInvoicesByUser(@PathVariable Long userId) {
//        List<InvoiceDTO> invoices = invoiceService.getInvoicesByUser(userId);
//        return ResponseEntity.ok(invoices);
//    }
//
//    @GetMapping("/attendances")
//    public ResponseEntity<List<AttendanceDTO>> getAttendanceByUser(@PathVariable Long userId) {
//        List<AttendanceDTO> attendances = lessonService.getAttendanceByUser(userId);
//        return ResponseEntity.ok(attendances);
//    }

}

