package com.revhire.controller;

import com.revhire.model.User;
import com.revhire.model.Education;
import com.revhire.model.Experience;
import com.revhire.model.Skill;
import com.revhire.model.Project;
import com.revhire.model.Certification;
import com.revhire.model.SavedJob;
import com.revhire.model.Company;
import com.revhire.repository.UserRepository;
import com.revhire.repository.EducationRepository;
import com.revhire.repository.ExperienceRepository;
import com.revhire.repository.SkillRepository;
import com.revhire.repository.ProjectRepository;
import com.revhire.repository.CertificationRepository;
import com.revhire.repository.SavedJobRepository;
import com.revhire.repository.CompanyRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.util.List;

@Controller
@RequestMapping("/profile")
public class ProfileController {

    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private EducationRepository educationRepository;
    
    @Autowired
    private ExperienceRepository experienceRepository;
    
    @Autowired
    private SkillRepository skillRepository;
    
    @Autowired
    private ProjectRepository projectRepository;
    
    @Autowired
    private CertificationRepository certificationRepository;
    
    @Autowired
    private SavedJobRepository savedJobRepository;

    @Autowired
    private CompanyRepository companyRepository;

    // ==================== MAIN PROFILE VIEW ====================
    
    @GetMapping
    public String viewProfile(Model model) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmail(email).orElse(null);
        
        if (user == null) {
            return "redirect:/auth/login";
        }
        
        // Handle Employer Profile (Company Profile)
        if ("EMPLOYER".equals(user.getUserType())) {
            Company company = companyRepository.findByUserId(user.getId()).orElse(new Company());
            model.addAttribute("company", company);
            model.addAttribute("user", user);
            return "employer/company-view";
        }
        
        model.addAttribute("user", user);
        
        // Get all Job Seeker data
        List<Education> educationList = educationRepository.findByUserIdOrderByEndYearDesc(user.getId());
        List<Experience> experienceList = experienceRepository.findByUserIdOrderByEndYearDesc(user.getId());
        List<Skill> skillList = skillRepository.findByUserId(user.getId());
        List<Project> projectList = projectRepository.findByUserId(user.getId());
        List<Certification> certificationList = certificationRepository.findByUserId(user.getId());
        List<SavedJob> savedJobsList = savedJobRepository.findByUserId(user.getId());
        
        model.addAttribute("educationList", educationList);
        model.addAttribute("experienceList", experienceList);
        model.addAttribute("skillList", skillList);
        model.addAttribute("projectList", projectList);
        model.addAttribute("certificationList", certificationList);
        model.addAttribute("savedJobsCount", savedJobsList.size());
        
        return "profile/view";
    }

    @GetMapping("/candidate/{id}")
    public String viewCandidateProfile(@PathVariable Long id, Model model) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User currentUser = userRepository.findByEmail(email).orElse(null);
        
        if (currentUser == null || !"EMPLOYER".equals(currentUser.getUserType())) {
            return "redirect:/auth/login";
        }
        
        User candidate = userRepository.findById(id).orElse(null);
        if (candidate == null) {
            return "redirect:/employer/dashboard";
        }
        
        model.addAttribute("user", candidate);
        
        // Get all candidate data
        List<Education> educationList = educationRepository.findByUserIdOrderByEndYearDesc(candidate.getId());
        List<Experience> experienceList = experienceRepository.findByUserIdOrderByEndYearDesc(candidate.getId());
        List<Skill> skillList = skillRepository.findByUserId(candidate.getId());
        List<Project> projectList = projectRepository.findByUserId(candidate.getId());
        List<Certification> certificationList = certificationRepository.findByUserId(candidate.getId());
        
        model.addAttribute("educationList", educationList);
        model.addAttribute("experienceList", experienceList);
        model.addAttribute("skillList", skillList);
        model.addAttribute("projectList", projectList);
        model.addAttribute("certificationList", certificationList);
        
        return "profile/candidate-view";
    }
    
    // ==================== UPDATE PROFILE ====================
    
    @GetMapping("/edit")
    public String showEditProfileForm(Model model) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmail(email).orElse(null);
        
        if (user == null) {
            return "redirect:/auth/login";
        }
        
        model.addAttribute("user", user);
        model.addAttribute("employmentStatuses", new String[]{"EMPLOYED", "UNEMPLOYED", "STUDENT", "FREELANCER"});
        
        return "profile/edit";
    }
    
    @PostMapping("/update")
    public String updateProfile(@RequestParam String firstName,
                               @RequestParam String lastName,
                               @RequestParam(required = false) String phone,
                               @RequestParam String location,
                               @RequestParam String employmentStatus,
                               @RequestParam(required = false) String profileHeadline,
                               @RequestParam(required = false) String summary,
                               @RequestParam(required = false) String preferredRoles,
                               RedirectAttributes redirectAttributes) {
        
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmail(email).orElse(null);
        
        if (user != null) {
            user.setFirstName(firstName);
            user.setLastName(lastName);
            user.setPhone(phone);
            user.setLocation(location);
            user.setEmploymentStatus(employmentStatus);
            user.setProfileHeadline(profileHeadline);
            user.setSummary(summary);
            user.setPreferredRoles(preferredRoles);
            userRepository.save(user);
            redirectAttributes.addFlashAttribute("success", "Profile updated successfully!");
        }
        
        return "redirect:/profile";
    }
    
    // ==================== EDUCATION ====================
    
    @GetMapping("/education/add")
    public String showEducationForm(Model model) {
        model.addAttribute("education", new Education());
        return "profile/education-form";
    }
    
    @PostMapping("/education/save")
    public String saveEducation(@ModelAttribute Education education, 
                               @RequestParam(required = false) Integer startYear,
                               @RequestParam(required = false) Integer endYear,
                               @RequestParam(required = false) String degree,
                               @RequestParam(required = false) String institution,
                               @RequestParam(required = false) String fieldOfStudy,
                               @RequestParam(required = false) String grade,
                               @RequestParam(required = false) String description,
                               RedirectAttributes redirectAttributes) {
        
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmail(email).orElse(null);
        
        if (user != null) {
            education.setUser(user);
            education.setDegree(degree);
            education.setInstitution(institution);
            education.setFieldOfStudy(fieldOfStudy);
            education.setStartYear(startYear);
            education.setEndYear(endYear);
            education.setGrade(grade);
            education.setDescription(description);
            educationRepository.save(education);
            redirectAttributes.addFlashAttribute("success", "Education added successfully!");
        }
        
        return "redirect:/profile";
    }
    
    @PostMapping("/education/delete/{id}")
    public String deleteEducation(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        educationRepository.deleteById(id);
        redirectAttributes.addFlashAttribute("success", "Education deleted successfully!");
        return "redirect:/profile";
    }
    
    // ==================== EXPERIENCE ====================
    
    @GetMapping("/experience/add")
    public String showExperienceForm(Model model) {
        model.addAttribute("experience", new Experience());
        return "profile/experience-form";
    }
    
    @PostMapping("/experience/save")
    public String saveExperience(@ModelAttribute Experience experience,
                                @RequestParam(required = false) Integer startYear,
                                @RequestParam(required = false) Integer endYear,
                                @RequestParam(value = "currentJob", required = false) String currentJob,
                                RedirectAttributes redirectAttributes) {
        
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmail(email).orElse(null);
        
        if (user != null) {
            experience.setUser(user);
            experience.setStartYear(startYear);
            experience.setEndYear(endYear);
            experience.setCurrentJob(currentJob != null);
            experienceRepository.save(experience);
            redirectAttributes.addFlashAttribute("success", "Experience added successfully!");
        }
        
        return "redirect:/profile";
    }
    
    @PostMapping("/experience/delete/{id}")
    public String deleteExperience(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        experienceRepository.deleteById(id);
        redirectAttributes.addFlashAttribute("success", "Experience deleted successfully!");
        return "redirect:/profile";
    }
    
    // ==================== SKILLS ====================
    
    @PostMapping("/skills/add")
    public String addSkill(@RequestParam String skillName, RedirectAttributes redirectAttributes) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmail(email).orElse(null);
        
        if (user != null && skillName != null && !skillName.trim().isEmpty()) {
            Skill skill = new Skill();
            skill.setUser(user);
            skill.setName(skillName.trim());
            skillRepository.save(skill);
            redirectAttributes.addFlashAttribute("success", "Skill added successfully!");
        }
        
        return "redirect:/profile";
    }
    
    @PostMapping("/skills/delete/{id}")
    public String deleteSkill(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        skillRepository.deleteById(id);
        redirectAttributes.addFlashAttribute("success", "Skill deleted successfully!");
        return "redirect:/profile";
    }
    
    // ==================== PROJECTS ====================
    
    @GetMapping("/project/add")
    public String showProjectForm(Model model) {
        model.addAttribute("project", new Project());
        return "profile/project-form";
    }
    
    @PostMapping("/project/save")
    public String saveProject(@ModelAttribute Project project,
                             @RequestParam(required = false) Integer startYear,
                             @RequestParam(required = false) Integer endYear,
                             @RequestParam(value = "currentProject", required = false) String currentProject,
                             RedirectAttributes redirectAttributes) {
        
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmail(email).orElse(null);
        
        if (user != null) {
            project.setUser(user);
            project.setStartYear(startYear);
            project.setEndYear(endYear);
            project.setCurrentProject(currentProject != null);
            projectRepository.save(project);
            redirectAttributes.addFlashAttribute("success", "Project added successfully!");
        }
        
        return "redirect:/profile";
    }
    
    @PostMapping("/project/delete/{id}")
    public String deleteProject(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        projectRepository.deleteById(id);
        redirectAttributes.addFlashAttribute("success", "Project deleted successfully!");
        return "redirect:/profile";
    }
    
    // ==================== CERTIFICATIONS ====================
    
  @GetMapping("/certification/add")
public String showCertificationForm(Model model) {
    model.addAttribute("certification", new Certification());
    return "profile/certification-form";
} 
 @PostMapping("/certification/save")
public String saveCertification(@RequestParam String name,
                               @RequestParam(required = false) String issuingOrganization,
                               RedirectAttributes redirectAttributes) {
    
    String email = SecurityContextHolder.getContext().getAuthentication().getName();
    User user = userRepository.findByEmail(email).orElse(null);
    
    if (user == null) {
        redirectAttributes.addFlashAttribute("error", "User not found");
        return "redirect:/profile";
    }
    
    try {
        Certification certification = new Certification();
        certification.setUser(user);
        certification.setName(name);
        certification.setIssuingOrganization(issuingOrganization);
        
        certificationRepository.save(certification);
        redirectAttributes.addFlashAttribute("success", "Certification added successfully!");
    } catch (Exception e) {
        redirectAttributes.addFlashAttribute("error", "Error: " + e.getMessage());
    }
    
    return "redirect:/profile";
}    

    @PostMapping("/certification/delete/{id}")
    public String deleteCertification(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        certificationRepository.deleteById(id);
        redirectAttributes.addFlashAttribute("success", "Certification deleted successfully!");
        return "redirect:/profile";
    }



    // ==================== PROFILE COMPLETENESS ====================
    
    @GetMapping("/completeness")
    @ResponseBody
    public String getProfileCompleteness() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmail(email).orElse(null);
        
        if (user == null) {
            return "0%";
        }
        
        int totalSections = 6;
        int completedSections = 0;
        
        if (user.getProfileHeadline() != null && !user.getProfileHeadline().isEmpty()) completedSections++;
        if (user.getSummary() != null && !user.getSummary().isEmpty()) completedSections++;
        if (!educationRepository.findByUserId(user.getId()).isEmpty()) completedSections++;
        if (!experienceRepository.findByUserId(user.getId()).isEmpty()) completedSections++;
        if (!skillRepository.findByUserId(user.getId()).isEmpty()) completedSections++;
        if (!projectRepository.findByUserId(user.getId()).isEmpty()) completedSections++;
        
        int percentage = (completedSections * 100) / totalSections;
        return percentage + "%";
    }
}