package engine.caesar.exception.parse;

public class ExcludedArgumentException extends Exception {

    public ExcludedArgumentException( String excluded, String included ) {

        super( String.format( "Argument '%s' cannot be used since alternative '%s' is already included.", excluded, included ) );

    }

}
