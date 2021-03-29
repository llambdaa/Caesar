package engine.caesar.exception.parse;

public class EssentialArgumentMissingException extends Exception {

    public EssentialArgumentMissingException( int index ) {

        super( String.format( "Essential argument at index '%s' is missing.", index ) );

    }

    public EssentialArgumentMissingException( String identifier ) {

        super( String.format( "Essential argument with identifier '%s' is missing.", identifier ) );

    }

}
