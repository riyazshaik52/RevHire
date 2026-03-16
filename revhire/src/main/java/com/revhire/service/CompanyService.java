package com.revhire.service;

import com.revhire.model.Company;
import com.revhire.repository.CompanyRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.Optional;

@Service
public class CompanyService {

    @Autowired
    private CompanyRepository companyRepository;

    public Company createCompany(Company company) {
        return companyRepository.save(company);
    }

    public Company updateCompany(Company company) {
        return companyRepository.save(company);
    }

    public Optional<Company> getCompanyByUserId(Long userId) {
        return companyRepository.findByUserId(userId);
    }

    public Optional<Company> getCompanyByName(String name) {
        return companyRepository.findByName(name);
    }
}