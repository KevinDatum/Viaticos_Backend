package com.viaticos.backend_viaticos.service;

import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class SseNotificationService {

    // 📣 La lista global de gerentes conectados
    private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();

    // 📡 Método para que los gerentes se conecten
    public SseEmitter crearConexion() {
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE); // Conexión infinita
        this.emitters.add(emitter);

        emitter.onCompletion(() -> this.emitters.remove(emitter));
        emitter.onTimeout(() -> this.emitters.remove(emitter));

        return emitter;
    }

    // 🔊 Método para gritarle a todos los gerentes (Cualquier Controller puede
    // llamar a este método)
    public void notificarCambioEnGastos() {
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event().name("message").data("ACTUALIZACION"));
            } catch (IOException e) {
                emitters.remove(emitter);
            }
        }
    }

}
