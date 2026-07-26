package ar.edu.itba.paw.webapp.DTO.Input;

import ar.edu.itba.paw.model.DTO.ImageDTO;
import org.glassfish.jersey.media.multipart.FormDataBodyPart;

public final class ImageInputDTO {

    private ImageInputDTO() {
        throw new AssertionError();
    }

    public static ImageDTO fromMultipart(byte[] imageBytes, FormDataBodyPart imagePart) {
        if (imagePart == null) {
            return new ImageDTO(null, null, 0, imageBytes);
        }

        String contentType = imagePart.getMediaType() == null ? null : imagePart.getMediaType().toString();
        String filename = imagePart.getContentDisposition() == null ? null : imagePart.getContentDisposition().getFileName();
        int sizeBytes = imageBytes == null ? 0 : imageBytes.length;

        return new ImageDTO(filename, contentType, sizeBytes, imageBytes);
    }
}
