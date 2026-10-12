package com.agri.cropemergency.controller;

import com.agri.cropemergency.dto.FieldRequest;
import com.agri.cropemergency.model.FarmField;
import com.agri.cropemergency.repository.FarmFieldRepository;
import com.agri.cropemergency.service.CropProfiles;
import com.agri.cropemergency.service.FieldService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class FieldController {

    private final FarmFieldRepository fields;
    private final FieldService fieldService;

    public FieldController(FarmFieldRepository fields, FieldService fieldService) {
        this.fields = fields;
        this.fieldService = fieldService;
    }

    @GetMapping("/crops")
    public List<String> crops() {
        return CropProfiles.names();
    }

    @GetMapping("/fields")
    public List<FarmField> list(Authentication auth) {
        if (fieldService.isAdmin(auth)) return fields.findAll();
        return fields.findByOwnerIdOrderByIdDesc(fieldService.currentUser(auth).getId());
    }

    @GetMapping("/fields/{id}")
    public FarmField get(@PathVariable Long id, Authentication auth) {
        return fieldService.getAccessible(id, auth);
    }

    @PostMapping("/fields")
    @ResponseStatus(HttpStatus.CREATED)
    public FarmField create(@Valid @RequestBody FieldRequest req, Authentication auth) {
        FarmField f = new FarmField();
        apply(f, req);
        f.setOwner(fieldService.currentUser(auth));
        return fields.save(f);
    }

    @PutMapping("/fields/{id}")
    public FarmField update(@PathVariable Long id, @Valid @RequestBody FieldRequest req, Authentication auth) {
        FarmField f = fieldService.getAccessible(id, auth);
        apply(f, req);
        return fields.save(f);
    }

    @DeleteMapping("/fields/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id, Authentication auth) {
        fieldService.deleteCascade(fieldService.getAccessible(id, auth));
    }

    private void apply(FarmField f, FieldRequest req) {
        f.setName(req.name().trim());
        f.setCropType(req.cropType());
        f.setLatitude(req.latitude());
        f.setLongitude(req.longitude());
        f.setAreaAcres(req.areaAcres());
    }
}
