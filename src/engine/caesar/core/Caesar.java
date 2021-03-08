package engine.caesar.core;

import engine.caesar.arg.*;
import engine.caesar.exception.*;

import java.util.*;
import java.util.stream.Collectors;

public class Caesar {

    /**
     * Parsing rules are applied for different kinds of arguments.
     * Fields are anonymous values and are accessed using their index -
     * their rules underlying schemes are stored in @INDEXED_RULES.
     * Annotated arguments are identified arguments like flags - their
     * identifier is stored along with the arguments abstraction in @ANNOTATED_RULES.
     */
    private static List< Scheme > INDEXED_RULES;
    private static Map< String, AnnotatedArgument > ANNOTATED_RULES;

    /**
     * Fetching results differ depending on the rule set.
     * Fields are accessed via their index from @INDEXED_FETCH because they are anonymous and
     * cannot be found using a name.
     * Annotated arguments on the other hand are identified and hence their values can be
     * retrieved from @ANNOTATED_FETCH providing their name.
     */
    private static List< String > INDEXED_FETCH;
    private static Map< String, String > ANNOTATED_FETCH;

    /**
     * Calibrating the parser is an important step, because
     * it stores the parsing rules and validates them (checks for
     * inconsistencies).
     *
     * Inconsistencies could be:
     * 1) duplicate fixed indices
     * 2) incoherent indices (not directly following each other)
     */
    public static void calibrate( List< Argument > arguments ) {

        /* 1. Step: Retrieving field definitions and validating them.
         * ----------------------------------------------------------
         * Validation includes checking if indices are coherent.
         * If their are no duplicates, the sum of the fields indices must be:
         *
         *      sum = 0 + 1 + 2 + ... + n     [Little Gauß]
         *
         * when n = fields.size - 1.
         * If the sum doesn't equal to term, there must be duplicates.
         * In this case, an exception is raised.
         *
         * Sorting the collection of fields is necessary because otherwise
         * field definitions won't definitely land at their right index.
         * Furthermore it makes the process of finding errors like index clashes
         * easier.
         */
        List< Field > fields = arguments.stream()
                                        .filter( arg -> arg instanceof Field )
                                        .map( element -> (Field) element )
                                        .sorted( Comparator.comparingInt(Argument::getIndex) )
                                        .collect( Collectors.toList() );

        //Duplication check.
        try {

            int sum = fields.stream().mapToInt( Argument::getIndex ).sum();
            int n   = fields.size() - 1;

            //Indices are coherent.
            if ( sum == ( n*(n+1) / 2D ) ) {

                Caesar.INDEXED_RULES = fields.stream()
                                             .map( Field::getScheme )
                                             .collect( Collectors.toList() );

            } else {

                /* Index incoherence is detected by checking if the delta
                 * between two consecutive indices is greater than one.
                 * Since the collection is sorted, duplicate elements will
                 * consecutive elements - hence, duplications can also easily
                 * be detected by comparing the last element to the current one.
                 */
                int last = -1;
                for ( Field field : fields ) {

                    int element = field.getIndex();
                    if ( last != element ) {

                        if ( element - last > 1 ) {

                            throw new IncoherentArgumentIndexException( element, last + 1 );

                        } else last = element;

                    } else throw new ArgumentIndexClashException( element );

                }

            }

        } catch ( ArgumentIndexClashException | IncoherentArgumentIndexException exception ) {

            exception.printStackTrace();

        }

        /* 2. Step: Storing annotated arguments and transferring alternatives
         *          and checking for duplicate definitions.
         * -------------------------------------------------------------------
         * If there are annotated arguments a and b, b can define a as an
         * alternative within its constructor.
         * Since b didn't exist when a was constructed, b is not explicitly
         * listed as an alternative for a - although technically it is.
         *
         * Hence, b is registered as an alternative for a.
         * This procedure allows for faster alternative search when parsing
         * the arguments.
         */
        //Storing and checking for duplicates.
        Caesar.ANNOTATED_RULES = new HashMap<>();
        arguments.forEach( arg -> {

            try {

                if ( arg instanceof AnnotatedArgument ) {

                    AnnotatedArgument annotated = ( AnnotatedArgument ) arg;
                    String identifier = annotated.getIdentifier();

                    if ( !Caesar.ANNOTATED_RULES.containsKey( identifier ) ) {

                        Caesar.ANNOTATED_RULES.put( identifier, annotated );

                    } else throw new DuplicateArgumentDefinitionException( identifier );

                }

            } catch ( DuplicateArgumentDefinitionException exception ) {

                exception.printStackTrace();

            }

        } );

        //Transferring alternative definitions.
        Caesar.ANNOTATED_RULES.forEach( ( identifier, annotated ) -> {

            /* Each of the elements from the entry's alternative collection
             * is addressed and the entry's key itself (identifier of currently
             * inspected annotated argument) is put into the respective element's
             * alternative collection.
             */
            annotated.getAlternatives().forEach( element -> {

                AnnotatedArgument alternative = Caesar.ANNOTATED_RULES.get( element.getIdentifier() );
                alternative.getAlternatives().add( annotated );

            } );

        } );

    }

    public static void fetch( String ... arguments ) {

        List< String > pool = Arrays.asList( arguments );

        /* 1. Step: Fetching field values and validating them.
         * ----------------------------------------------------------
         * Field values are written to the collection if they match
         * the provided scheme of the definition at the same index.
         *
         * If the collection of fields is smaller than the collection
         * of definitions, there is at least one field missing.
         * Therefore, an exception must be raised.
         */
        Caesar.INDEXED_FETCH = new ArrayList<>();
        try {

            for ( int i = 0; i < INDEXED_RULES.size(); i++ ) {

                //Checking if field with this index still exists.
                if ( i < pool.size() ) {

                    /* The access index is always zero, because an already validated
                     * value (the first one) is removed from the list, shifting the following
                     * element to its position.
                     */
                    String target = pool.get( 0 );

                    //Determining if field matches the scheme.
                    if ( INDEXED_RULES.get( i ).applies( target ) ) {

                        INDEXED_FETCH.add( target );
                        pool.remove( 0 );

                    } else throw new SchemeMismatchException( target );

                } else throw new EssentialArgumentMissingException( i );

            }

        } catch ( SchemeMismatchException | EssentialArgumentMissingException exception ) {

            exception.printStackTrace();

        }

        /* 2. Step: Fetching flags and groups and validating them
         *          (excluding dependencies).
         * ----------------------------------------------------------
         * Flags and groups are annotated arguments.
         * They are extracted and validated with respect to identifier values
         * and alternative labels.
         */
        try {

            if ( ANNOTATED_RULES.size() > 0 ) {

                /* @parent is defined if the flag of a group has been detected in the
                 * previous iteration - this group is considered the parent of the
                 * coming argument.
                 * It provides context to which scheme must be applied to the following
                 * argument.
                 */
                AnnotatedArgument parent = null;
                AnnotatedArgument candidate;

                //Iterating over remaining arguments after removing field values.
                for ( String element : pool ) {

                    /* Each iteration could yield a new annotated argument (simple flag or group),
                     * therefore, each time a possible candidate must be evaluated.
                     */
                    candidate = ANNOTATED_RULES.get( element );
                    if ( parent == null ) {

                        /* If @parent is undefined (the algorithm expects an annotated argument, so either
                         * a flag or a group) and the current argument doesn't announce such, then the argument
                         * is invalid and therefore an exception is thrown.
                         */
                        if ( candidate != null ) {

                            /* Some annotated arguments might have alternatives or aliases.
                             * They exclude each other, which means that if any alternative
                             * can be found in the cache collection, an exception is thrown
                             * because otherwise there would be multiple arguments that
                             * exclude each other.
                             */
                            boolean excluded = candidate.getAlternatives()
                                                        .stream()
                                                        .noneMatch( alt -> ANNOTATED_FETCH.containsKey( alt.getIdentifier() ) );
                            if ( !excluded ) {

                                if ( candidate instanceof Group ) {

                                    parent = candidate;

                                } else {

                                    ANNOTATED_FETCH.put( element, null );

                                }

                            } else {

                                AnnotatedArgument included = candidate.getAlternatives()
                                                                      .stream()
                                                                      .filter( alt -> ANNOTATED_FETCH.containsKey( alt.getIdentifier() ) )
                                                                      .findAny().get();
                                throw new ExcludedArgumentException( element, included.getIdentifier() );

                            }

                        } else throw new InvalidFlagException( element );

                    } else {

                        /* If the scheme applies to the current argument, the annotated argument
                         * is valid and the group is stored in the cache collection.
                         * Then, the @parent is set to null again to indicate that another
                         * annotated argument is expected.
                         * Otherwise, the current argument is invalid and an exception is thrown.
                         */
                        Scheme scheme = ( ( Group ) parent ).getScheme();
                        if ( scheme.applies( element ) ) {

                            ANNOTATED_FETCH.put( parent.getIdentifier(), element );
                            parent = null;

                        } else throw new SchemeMismatchException( element );

                    }

                }

            }

        } catch ( InvalidFlagException | ExcludedArgumentException | SchemeMismatchException exception ) {

            exception.printStackTrace();

        }

        /* 3. Step: Evaluating dependencies and their fulfillment.
         * ----------------------------------------------------------
         * Annotated arguments can have dependencies which must be evaluated.
         * If an argument doesn't have its dependencies fulfilled, an
         * exception is thrown.
         */
        try {

            for ( String argument : ANNOTATED_FETCH.keySet() ) {

                List< AnnotatedArgument > dependencies = ANNOTATED_RULES.get( argument ).getDependencies();
                for ( AnnotatedArgument dependency : dependencies ) {

                    String identifier = dependency.getIdentifier();
                    if ( !ANNOTATED_FETCH.containsKey( identifier ) ) {

                        throw new UnfulfilledArgumentDependencyException( argument, identifier );

                    }

                }

            }

        } catch ( UnfulfilledArgumentDependencyException exception ) {

            exception.printStackTrace();

        }

        /* x. Step: Checking if all essential arguments are provided.
         * ----------------------------------------------------------
         */
        try {

            for ( Map.Entry< String, AnnotatedArgument > entry : ANNOTATED_RULES.entrySet() ) {

                if ( entry.getValue().isEssential() && !ANNOTATED_FETCH.containsKey( entry.getKey() ) ) {

                    throw new EssentialArgumentMissingException( entry.getKey() );

                }

            }

        } catch ( EssentialArgumentMissingException exception ) {

            exception.printStackTrace();

        }

    }

    public static String getValue( int index ) {

        return Caesar.INDEXED_FETCH.get( index );

    }

    public static String getValue( String identifier ) {

        return Caesar.ANNOTATED_FETCH.get( identifier );

    }

    public static boolean isPresent( String identifier ) {

        return Caesar.ANNOTATED_FETCH.containsKey( identifier );

    }

    public static void main( String ... arguments ) {

        //TODO REMOVE TESTING
        List< Argument > args = new ArrayList<>();
        args.add( new Field( Scheme.INTEGER, 0 ) );
        args.add( new Field( Scheme.INTEGER, 1 ) );
        Flag a = new Flag( false, "-r", null, null );
        args.add( a );
        Flag b = new Flag( false, "-b", Collections.singletonList( a ), null );
        args.add( b );

        Caesar.calibrate( args );
        String[] lol = new String[]{ "0" };
        Caesar.fetch( lol );

    }

}
