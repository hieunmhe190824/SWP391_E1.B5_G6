package com.carrental.service;

import com.carrental.model.Handover;
import com.carrental.repository.HandoverRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class HandoverService {

    @Autowired
    private HandoverRepository handoverRepository;

    public List<Handover> getAllHandovers() {
        return handoverRepository.findAll();
    }

    public Optional<Handover> getHandoverById(Long id) {
        return handoverRepository.findById(id);
    }

    public List<Handover> getHandoversByContract(Long contractId) {
        return handoverRepository.findByContractId(contractId);
    }

    public Handover createHandover(Handover handover) {
        handover.setHandoverTime(LocalDateTime.now());
        return handoverRepository.save(handover);
    }

    public Handover updateHandover(Long id, Handover handoverDetails) {
        Handover handover = handoverRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Handover not found"));

        handover.setOdometer(handoverDetails.getOdometer());
        handover.setFuelLevel(handoverDetails.getFuelLevel());
        handover.setConditionNotes(handoverDetails.getConditionNotes());
        handover.setImages(handoverDetails.getImages());

        return handoverRepository.save(handover);
    }
}
