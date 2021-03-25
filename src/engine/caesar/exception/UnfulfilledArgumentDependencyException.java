package engine.caesar.exception;

public class UnfulfilledArgumentDependencyException extends Exception {

    public UnfulfilledArgumentDependencyException( String target, String dependency ) {

        super( String.format( "Argument '%s' is missing dependency argument '%s'.", target, dependency ) );

    }

}
