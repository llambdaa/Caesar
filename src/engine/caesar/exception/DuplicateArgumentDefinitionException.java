package engine.caesar.exception;

public class DuplicateArgumentDefinitionException extends Exception {

    public DuplicateArgumentDefinitionException( String identifier ) {

        super( String.format( "Argument '%s' has duplicate definitions.", identifier ) );

    }

}
