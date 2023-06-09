package br.edu.ifpb.pweb2.pederneira.controller;

import br.edu.ifpb.pweb2.pederneira.model.Enrollment;
import br.edu.ifpb.pweb2.pederneira.model.Institution;
import br.edu.ifpb.pweb2.pederneira.model.Semester;
import br.edu.ifpb.pweb2.pederneira.model.Student;
import br.edu.ifpb.pweb2.pederneira.repository.EnrollmentRepository;
import br.edu.ifpb.pweb2.pederneira.repository.InstitutionRepository;
import br.edu.ifpb.pweb2.pederneira.repository.SemesterRepository;
import br.edu.ifpb.pweb2.pederneira.repository.StudentRepository;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Optional;

@Controller
@RequestMapping("/institution")
public class InstitutionController {

    @Resource
    private InstitutionRepository institutionRepository;
    @Resource
    private SemesterRepository semesterRepository;
    @Resource
    private StudentRepository studentRepository;
    @Resource
    private EnrollmentRepository enrollmentRepository;

    @GetMapping
    public ModelAndView getHome(
        ModelAndView mav,
        @RequestParam(defaultValue = "1") int page
    ) {
        int size = 3;
        Pageable paging = PageRequest.of(page - 1, size);
        mav.addObject("institutions", this.institutionRepository.findAll(paging));
        mav.setViewName("layouts/institution/home");
        return mav;
    }

    @GetMapping("/create")
    public ModelAndView getCreatePage(ModelAndView mav) {
        mav.addObject("institution", new Institution());
        mav.setViewName("layouts/institution/create");
        return mav;
    }

    @PostMapping("/create")
    public ModelAndView create(@Valid Institution institution, BindingResult bindingResult, ModelAndView mav, RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            mav.setViewName("layouts/institution/create");
            return mav;
        }

        if (institution.getId() != null && this.institutionRepository.findById(institution.getId()).isPresent()) {
            redirectAttributes.addFlashAttribute("error", "Instituição já cadastrada");
            mav.setViewName("redirect:/institution");
            return mav;
        }

        this.institutionRepository.save(institution);

        mav.setViewName("redirect:/institution");
        return mav;
    }

    @GetMapping("/update/{id}")
    public ModelAndView getUpdatePage(@PathVariable(name = "id") Integer id, ModelAndView mav, RedirectAttributes redirectAttributes) {
        Optional<Institution> institution = this.institutionRepository.findById(id);

        if (institution.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Instituição não encontrada");
            mav.setViewName("redirect:/institution");
            return mav;
        }

        mav.addObject("institution", institution.get());
        mav.setViewName("layouts/institution/update");
        return mav;
    }

    @PutMapping("/update")
    public ModelAndView update(@Valid Institution institution, BindingResult bindingResult, ModelAndView mav, RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            mav.addObject("semesters", this.semesterRepository.findAll());
            mav.setViewName("layouts/institution/update");
            return mav;
        }

        Optional<Institution> institutionOptional = this.institutionRepository.findById(institution.getId());

        if (institutionOptional.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Instituição não encontrada");
            mav.setViewName("redirect:/institution");
            return mav;
        }

        Institution institutionToUpdate = institutionOptional.get();

        if (institution.getCurrentSemester() != null) {
            Optional<Semester> semester = this.semesterRepository.findById(institution.getCurrentSemester().getId());

            if (semester.isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "Semestre atual não encontrado para instituição");
                mav.setViewName("redirect:/institution");
                return mav;
            }

            institutionToUpdate.setCurrentSemester(semester.get());
        }

        institutionToUpdate.setName(institution.getName());
        institutionToUpdate.setAcronym(institution.getAcronym());
        institutionToUpdate.setPhone(institution.getPhone());

        this.institutionRepository.save(institutionToUpdate);

        mav.setViewName("redirect:/institution");
        return mav;
    }
    
    @GetMapping("/delete/{id}")
    private ModelAndView delete(@PathVariable(name = "id") Integer id, ModelAndView mav) {
        Optional<Institution> institutionOptional = this.institutionRepository.findById(id);

        if (institutionOptional.isEmpty()) {
            mav.setViewName("redirect:/institution");
            return mav;
        }

        Institution institution = institutionOptional.get();

        for (Student student : institution.getStudents()) {
            student.setCurrentInstitution(null);
            student.setCurrentEnrollment(null);
        }

        for (Semester semester : institution.getSemesters()) {
            this.enrollmentRepository.deleteAll(this.enrollmentRepository.findBySemesterId(semester.getId()));
        }

        this.studentRepository.saveAll(institution.getStudents());

        this.institutionRepository.deleteById(id);

        mav.setViewName("redirect:/institution");
        return mav;
    }

}
