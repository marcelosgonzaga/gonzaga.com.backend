package gonzaga.jornalfacil.exception;

public class EntityAlreadyExistsException extends RuntimeException  {
    public EntityAlreadyExistsException(String message) {
        super(message);
    }
}
