package com.gymiq.service;

import com.gymiq.dto.request.CreateStudentRequest;
import com.gymiq.dto.response.StudentResponse;
import com.gymiq.entity.Student;
import com.gymiq.entity.User;
import com.gymiq.exception.BusinessException;
import com.gymiq.repository.StudentRepository;
import com.gymiq.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StudentServiceTest {

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private StudentDataService studentDataService;

    @InjectMocks
    private StudentService studentService;

    @Test
    void createShouldPersistStudentWithResolvedAddress() {
        CreateStudentRequest request = validCreateStudentRequest();

        doNothing().when(studentDataService).validateCpf(request.getCpf());
        when(userRepository.existsByEmail(request.getEmail())).thenReturn(false);
        when(studentRepository.existsByCpf(request.getCpf())).thenReturn(false);
        when(passwordEncoder.encode(request.getPassword())).thenReturn("encoded-password");
        when(studentDataService.resolveAddress(request.getZipCode(), request.getAddress()))
                .thenReturn("Praca da Se, Sao Paulo - SP");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setUserId(10);
            return user;
        });
        when(studentRepository.save(any(Student.class))).thenAnswer(invocation -> {
            Student student = invocation.getArgument(0);
            student.setStudentId(1);
            return student;
        });

        StudentResponse response = studentService.create(request);

        assertThat(response.getStudentId()).isEqualTo(1);
        assertThat(response.getEmail()).isEqualTo("ana@gymiq.com");
        assertThat(response.getAddress()).isEqualTo("Praca da Se, Sao Paulo - SP");
        verify(userRepository).save(any(User.class));
        verify(studentRepository).save(any(Student.class));
    }

    @Test
    void createShouldRejectDuplicatedEmail() {
        CreateStudentRequest request = validCreateStudentRequest();

        doNothing().when(studentDataService).validateCpf(request.getCpf());
        when(userRepository.existsByEmail(request.getEmail())).thenReturn(true);

        assertThatThrownBy(() -> studentService.create(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("E-mail");

        verify(studentRepository, never()).save(any(Student.class));
    }

    private CreateStudentRequest validCreateStudentRequest() {
        CreateStudentRequest request = new CreateStudentRequest();
        request.setName("Ana Silva");
        request.setEmail("ana@gymiq.com");
        request.setPassword("secret123");
        request.setCpf("123.456.789-09");
        request.setBirthDate(LocalDate.of(2000, 1, 15));
        request.setPhone("11999999999");
        request.setZipCode("01001-000");
        request.setAddress("Endereco manual");
        return request;
    }
}
