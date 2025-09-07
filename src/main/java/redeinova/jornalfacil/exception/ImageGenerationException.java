package redeinova.jornalfacil.exception;

public class ImageGenerationException extends RuntimeException {
    public ImageGenerationException(String message, Throwable cause) {
        super(message, cause);
    }

    public ImageGenerationException(String message) {
        super(message);
    }
}