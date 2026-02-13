package com.viaticos.backend_viaticos.service.storage;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.util.Iterator;
import java.util.UUID;

@Service
public class StorageService {

    /**
     * Convierte una imagen (jpg/png/etc) a WEBP y retorna los bytes.
     */
    public byte[] convertToWebpBytes(MultipartFile file) throws IOException {

        if (file == null || file.isEmpty()) {
            throw new RuntimeException("Archivo vacío o no recibido");
        }

        BufferedImage original = ImageIO.read(file.getInputStream());
        if (original == null) {
            throw new RuntimeException("No se pudo leer la imagen. Formato no soportado.");
        }

        // Convertir a un formato seguro (RGB)
        BufferedImage image = new BufferedImage(
                original.getWidth(),
                original.getHeight(),
                BufferedImage.TYPE_INT_RGB);

        image.getGraphics().drawImage(original, 0, 0, null);

        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("webp");
        if (!writers.hasNext()) {
            throw new RuntimeException("No hay soporte WebP instalado. Verifica la dependencia webp-imageio.");
        }

        ImageWriter writer = writers.next();

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ImageOutputStream ios = ImageIO.createImageOutputStream(baos)) {

            writer.setOutput(ios);

            ImageWriteParam param = writer.getDefaultWriteParam();
            param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);

            String[] compressionTypes = param.getCompressionTypes();
            if (compressionTypes != null && compressionTypes.length > 0) {
                param.setCompressionType(compressionTypes[0]); // "Lossy"
            }

            param.setCompressionQuality(0.80f);

            writer.write(null, new IIOImage(image, null, null), param);

            ios.flush();

            byte[] result = baos.toByteArray();

            if (result == null || result.length == 0) {
                throw new RuntimeException("No se generó contenido WEBP (webpBytes vacío).");
            }

            return result;

        } finally {
            writer.dispose();
        }
    }

    /**
     * Genera un objectName recomendado para OCI.
     * Ejemplo: gastos/2026/02/uuid.webp
     */
    public String generateObjectName(String prefix) {

        LocalDate now = LocalDate.now();

        return String.format(
                "%s/%d/%02d/%s.webp",
                prefix,
                now.getYear(),
                now.getMonthValue(),
                UUID.randomUUID());
    }

    /**
     * Ya no aplica en OCI.
     */
    @Deprecated
    public void deleteByUrl(String url) {
        throw new UnsupportedOperationException("deleteByUrl ya no aplica. Usa OCI deleteObject.");
    }

    /**
     * Ya no aplica en OCI.
     */
    @Deprecated
    public boolean exists(String url) {
        throw new UnsupportedOperationException("exists ya no aplica. Usa OCI headObject.");
    }
}
