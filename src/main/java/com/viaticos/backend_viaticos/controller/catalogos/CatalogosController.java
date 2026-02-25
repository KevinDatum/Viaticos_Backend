package com.viaticos.backend_viaticos.controller.catalogos;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.viaticos.backend_viaticos.dto.response.CatalogoDTO;
import com.viaticos.backend_viaticos.dto.response.JefeDTO;
import com.viaticos.backend_viaticos.repository.CargoRepository;
import com.viaticos.backend_viaticos.repository.DepartamentoRepository;
import com.viaticos.backend_viaticos.repository.EmpleadoRepository;
import com.viaticos.backend_viaticos.repository.EmpresaRepository;
import com.viaticos.backend_viaticos.repository.PaisRepository;
import com.viaticos.backend_viaticos.repository.RolRepository;

@RestController
@RequestMapping("/catalogos")
@CrossOrigin(origins = "http://localhost:5173")
public class CatalogosController {

    @Autowired
    private DepartamentoRepository departamentoRepository;

    @Autowired
    private CargoRepository cargoRepository;

    @Autowired
    private EmpresaRepository empresaRepository;

    @Autowired
    private PaisRepository paisRepository;

    @Autowired
    private RolRepository rolRepository;

    @Autowired
    private EmpleadoRepository empleadoRepository;

    // ✅ Departamentos
    @GetMapping("/departamentos")
    public ResponseEntity<?> listarDepartamentos() {
        List<CatalogoDTO> data = departamentoRepository.findAll()
                .stream()
                .map(d -> new CatalogoDTO(d.getIdDepartamento(), d.getNombre()))
                .collect(Collectors.toList());

        return ResponseEntity.ok(data);
    }

    // ✅ Cargos
    @GetMapping("/cargos")
    public ResponseEntity<?> listarCargos() {
        List<CatalogoDTO> data = cargoRepository.findAll()
                .stream()
                .map(c -> new CatalogoDTO(c.getIdCargo(), c.getNombre()))
                .collect(Collectors.toList());

        return ResponseEntity.ok(data);
    }

    // ✅ Empresas
    @GetMapping("/empresas")
    public ResponseEntity<?> listarEmpresas() {
        List<CatalogoDTO> data = empresaRepository.findAll()
                .stream()
                .map(e -> new CatalogoDTO(e.getIdEmpresa(), e.getNombre()))
                .collect(Collectors.toList());

        return ResponseEntity.ok(data);
    }

    // ✅ Países
    @GetMapping("/paises")
    public ResponseEntity<?> listarPaises() {
        List<CatalogoDTO> data = paisRepository.findAll()
                .stream()
                .map(p -> new CatalogoDTO(p.getIdPais(), p.getNombre()))
                .collect(Collectors.toList());

        return ResponseEntity.ok(data);
    }

    // ✅ Roles
    @GetMapping("/roles")
    public ResponseEntity<?> listarRoles() {
        List<CatalogoDTO> data = rolRepository.findAll()
                .stream()
                .map(r -> new CatalogoDTO(r.getIdRol(), r.getNombre()))
                .collect(Collectors.toList());

        return ResponseEntity.ok(data);
    }

    // ✅ Jefes (empleados disponibles como jefe)
    @GetMapping("/jefes")
    public ResponseEntity<?> listarJefes() {
        List<JefeDTO> data = empleadoRepository.findAll()
                .stream()
                .map(e -> new JefeDTO(
                        e.getIdEmpleado(),
                        e.getNombre() + " " + e.getApellido(),
                        e.getCorreo()
                ))
                .collect(Collectors.toList());

        return ResponseEntity.ok(data);
    }
}
