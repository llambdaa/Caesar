package engine.caesar.exception.config;

public class DependencyClashExeption extends Exception {

    public DependencyClashExeption( String first, String second, String target ) {

        super( String.format( "Argument '%s' and '%s' cannot both be dependencies for '%s' because they are alternatives and hence exclude each other.", first, second, target ) );

    }

}
