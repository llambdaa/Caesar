package engine.caesar.core;

import engine.caesar.arg.*;
import engine.caesar.exception.*;

import java.util.*;
import java.util.stream.Collectors;

public class Caesar {

    /** Program arguments can be separated into two major groups:
     *      1. Fields: These are anonymous arguments that have a fixed index at
     *                 which they can be accessed.
     *      2. Arguments: These are the "real" and identified arguments, sometimes
     *                    also referred to by the name of "annotated arguments"
     *                    because they have an identifier or flag as an annotation before
     *                    the actual argument value.
     *
     *  Since these types are accessed in to very different manners (fields by indices
     *  and arguments by identifier), they are stored in separate data structures.
     *
     *  For the sake of computational simplicity for the complex process of parsing
     *  program arguments, after the main configuration, aliased arguments are grouped
     *  together and stored in a third structure, although it technically is redundant.
     *  But the form allows for faster comparison because aliases of a given argument
     *  don't have to be searched for each time they are to be used.
     */
    private static List< Field > FIELD_CONFIG;
    private static Map< String, AnnotatedArgument > ARG_CONFIG;
    private static Map< String, HashSet< AnnotatedArgument > > ARG_ALIASES;

//    /**
//     * Fetching results differ depending on the rule set.
//     * Fields are accessed via their index from @INDEXED_FETCH because they are anonymous and
//     * cannot be found using a name.
//     * Annotated arguments on the other hand are identified and hence their values can be
//     * retrieved from @ANNOTATED_FETCH providing their name.
//     */
//    private static List< String > INDEXED_FETCH;
//    private static Map< String, List< String > > ANNOTATED_FETCH;
//    private static Map< String, HashSet< String > > ALIASES;
//
//    /**
//     * Calibrating the parser is an important step, because
//     * it stores the parsing rules and validates them (checks for
//     * inconsistencies).
//     *
//     * Inconsistencies could be:
//     * 1) duplicate fixed indices
//     * 2) incoherent indices (not directly following each other)
//     */

    /** Caesar doesn't know how to parse the program arguments without
     *  a definite rule set - it must therefore first be configured before usage.
     */
    public static void configure( List< Argument > fragments ) {

        /* -------------------------------------------------------------
         * 1. Step: Finding field declarations and validating them.
         * ------------------------------------------------------------- */
        Caesar.FIELD_CONFIG = Caesar.getFieldConfigurations( fragments );

        /* -------------------------------------------------------------
         * 2. Step: Finding argument declarations and validating them.
         * ------------------------------------------------------------- */
        Caesar.ARG_CONFIG = Caesar.getArgumentConfigurations( fragments );

        /* -------------------------------------------------------------
         * 3. Step: Crosslinking related arguments.
         * ------------------------------------------------------------- */
        Caesar.crosslink( Caesar.ARG_CONFIG );

        /* -------------------------------------------------------------
         * 4. Step: Grouping together alternative or aliased arguments.
         * ------------------------------------------------------------- */
        Caesar.ARG_ALIASES = Caesar.groupAliases( Caesar.ARG_CONFIG );

        /* -------------------------------------------------------------
         * 5. Step: Aligning essentiality over whole alias groups.
         * ------------------------------------------------------------- */
        Caesar.alignEssentiality( Caesar.ARG_CONFIG, Caesar.ARG_ALIASES );

    }

    /** This method filters out indexed field arguments from the given
     *  collection and then validates them for index coherence before returning.
     */
    private static List< Field > getFieldConfigurations( List< Argument > fragments ) {

        List< Field > fields = fragments.stream()
                                        .filter( arg -> arg instanceof Field )
                                        .map( arg -> ( Field ) arg )
                                        .sorted( Comparator.comparingInt( Field::getIndex ) )
                                        .collect( Collectors.toList() );

        /* The algorithm iterates over the filtered out fields.
         * It inspects their claimed index and compares it to
         * the index of the field before.
         * If there is a duplicate index, there is definitely an index
         * clash, which is why an exception is thrown.
         * If, however, the indices have a greater delta than one, which
         * means that an index in between is not claimed by a field,
         * another exception is thrown.
         */
        try {

            int last = -1;
            for ( Field field : fields ) {

                int claim = field.getIndex();
                if ( claim == last ) {

                    throw new FieldClashException( claim );

                } else if ( claim - last > 1 ) {

                    throw new IncoherentFieldsException( claim, last + 1 );

                } else last = claim;

            }

        } catch ( FieldClashException | IncoherentFieldsException exception ) {

            exception.printStackTrace();

        }

        /* No field incoherence or index clash has been detected,
         * so the collection can be returned.
         */
        return fields;

    }

    /** This method filters out annotated arguments from the given
     *  collection and then searches for duplicates before returning.
     *
     *  Returning a map of an annotated argument with its identifier
     *  is redundant because the object holds the identifier itself
     *  but this also allows for theoretical faster access speeds
     *  when working with the parsed arguments.
     */
    private static Map< String, AnnotatedArgument > getArgumentConfigurations( List< Argument > fragments ) {

        Map< String, AnnotatedArgument > result = new HashMap<>();
        fragments.forEach( arg -> {

            try {

                if ( arg instanceof AnnotatedArgument ) {

                    AnnotatedArgument annotated = ( AnnotatedArgument ) arg;
                    String identifier = annotated.getIdentifier();
                    if ( !result.containsKey( identifier ) ) {

                        result.put( identifier, annotated );

                    } else throw new DuplicateArgumentDefinitionException( identifier );

                }

            } catch ( DuplicateArgumentDefinitionException exception ) {

                exception.printStackTrace();

            }

        } );

        return result;

    }

    /** This method crosslinks arguments by declaring them their respective
     *  alternative or alias if at least one of them has declared the other
     *  one as such - this is needed, so that arguments know of each other
     *  as an alias.
     *
     *  After the process an argument does only know about other arguments
     *  that are directly linked to it but not about the ones that are implicitly
     *  connected.
     *  Example: a (not linked) and b (linked to a)
     *
     *  If b knows about a because they are alternatives to each other, than a
     *  should also know about b as an alternative.
     *  Basically, unilateral connections are made bilateral.
     *  This ensures, that if you take any element from the hypothetical group,
     *  that this element knows about at least one other element of that group.
     *  This allows the group finding algorithm to wander around these direct
     *  connections to make out the whole group.
     */
    private static void crosslink( Map< String, AnnotatedArgument > arguments ) {

        arguments.values().forEach( arg ->  arg.getAlternatives().forEach( other -> {

            List< AnnotatedArgument > alternatives = other.getAlternatives();
            if ( !alternatives.contains( arg ) ) {

                alternatives.add( arg );

            }

        } ) );

    }

    /** This method groups together arguments that alias each other.
     *  Found groups are put into @Caesar.ALIASES and are later used
     *  to detect e.g. dependency clashes.
     */
    private static Map< String, HashSet< AnnotatedArgument > > groupAliases( Map< String, AnnotatedArgument > source ) {

        Map< String, AnnotatedArgument >         arguments = ( HashMap< String, AnnotatedArgument > ) ( ( HashMap ) source ).clone();
        Map< String, HashSet< AnnotatedArgument > > result = new HashMap<>();

        /* The algorithm takes the first element of @arguments and
         * groups it together with its aliases.
         * If an alias is found, it is removed from @arguments, so that
         * in the next major iteration, the first element is an alias
         * in another group.
         */
        while ( !arguments.isEmpty() ) {

            AnnotatedArgument              first = ( AnnotatedArgument ) arguments.values().toArray()[ 0 ];
            HashSet< AnnotatedArgument > aliases = new HashSet<>( Collections.singleton( first ) );
            Queue< AnnotatedArgument >     query = new LinkedList<>( first.getAlternatives() );

            /* When checking an annotated argument, its alternative arguments
             * are put on the queue, so that they themselves are checked later on.
             * This guarantees that at the end each element of the alias group
             * has been processed.
             */
            while ( !query.isEmpty() ) {

                AnnotatedArgument front = query.poll();
                String identifier       = front.getIdentifier();
                if ( arguments.containsKey( identifier ) ) {

                    arguments.remove( identifier );
                    aliases.add( front );
                    query.addAll( front.getAlternatives() );

                }

            }

            /* At this point, all elements of the alias group have been
             * found and it is ready to be outputted.
             * For faster search, each element of the group itself is linked
             * with the whole group in the resulting hashmap.
             */
            aliases.forEach( alias -> result.put( alias.getIdentifier(), aliases ) );

        }

        return result;

    }

    /** This method aligns essentiality values for each argument
     *  in an alias group.
     *
     *  This is an important step because an alternative to an essential argument
     *  cannot be non-essential. Alternative arguments exclude each other and if
     *  the non-essential argument would be used instead of the essential one,
     *  the engine would detect that an essential argument is missing.
     *  But at the same time, the essential argument cannot be used
     *  because it is excluded due to the non-essential argument being in use.
     */
    private static void alignEssentiality( Map< String, AnnotatedArgument > source, Map< String, HashSet< AnnotatedArgument > > aliases ) {

        Map< String, AnnotatedArgument > arguments = ( HashMap< String, AnnotatedArgument > ) ( ( HashMap ) source ).clone();
        while ( arguments.size() > 0 ) {

            AnnotatedArgument            first = ( AnnotatedArgument ) arguments.values().toArray()[ 0 ];
            HashSet< AnnotatedArgument > group = aliases.get( first.getIdentifier() );

            /* Essential is true if any of the elements of the alias group
             * itself is essential.
             * If it is indeed true, each argument must be declared essential.
             */
            boolean essential = group.stream().anyMatch( Argument::isEssential );
            group.forEach( arg -> {

                if ( essential ) {

                    arg.setEssential( true );

                }

                /* After evaluation and possible declaration of essentiality,
                 * each element of the alias group must be deleted from the map.
                 */
                arguments.remove( arg.getIdentifier() );

            } );

        }

    }

    public static void fetch( String ... arguments ) {

//        List< String > pool = Arrays.asList( arguments );
//        /* 1. Step: Fetching field values and validating them.
//         * ----------------------------------------------------------
//         * Field values are written to the collection if they match
//         * the provided scheme of the definition at the same index.
//         *
//         * If the collection of fields is smaller than the collection
//         * of definitions, there is at least one field missing.
//         * Therefore, an exception must be raised.
//         */
//        Caesar.INDEXED_FETCH = new ArrayList<>();
//        try {
//
//            for ( int i = 0; i < INDEXED_RULES.size(); i++ ) {
//
//                //Checking if field with this index still exists.
//                if ( i < pool.size() ) {
//
//                    /* The access index is always zero, because an already validated
//                     * value (the first one) is removed from the list, shifting the following
//                     * element to its position.
//                     */
//                    String target = pool.get( 0 );
//
//                    //Determining if field matches the scheme.
//                    if ( INDEXED_RULES.get( i ).applies( target ) ) {
//
//                        INDEXED_FETCH.add( target );
//                        pool.remove( 0 );
//
//                    } else throw new SchemeMismatchException( target );
//
//                } else throw new EssentialArgumentMissingException( i );
//
//            }
//
//        } catch ( SchemeMismatchException | EssentialArgumentMissingException exception ) {
//
//            exception.printStackTrace();
//
//        }
//
//        /* 2. Step: Fetching flags and groups and validating them
//         *          (excluding dependencies).
//         * ----------------------------------------------------------
//         * Flags and groups are annotated arguments.
//         * They are extracted and validated with respect to identifier values
//         * and alternative labels.
//         */
//        Caesar.ANNOTATED_FETCH = new HashMap<>();
//        try {
//
//            if ( ANNOTATED_RULES.size() > 0 ) {
//
//                /* @parent is defined if the flag of a group has been detected in the
//                 * previous iteration - this group is considered the parent of the
//                 * coming argument.
//                 * It provides context to which scheme must be applied to the following
//                 * argument.
//                 */
//                Group          parent = null;
//                List< String > values = null;
//                int    expectedValues = 0;
//
//                AnnotatedArgument candidate;
//
//                //Iterating over remaining arguments after removing field values.
//                for ( String element : pool ) {
//
//                    /* Each iteration could yield a new annotated argument (simple flag or group),
//                     * therefore, each time a possible candidate must be evaluated.
//                     */
//                    candidate = ANNOTATED_RULES.get( element );
//                    if ( parent == null ) {
//
//                        /* If @parent is undefined (the algorithm expects an annotated argument, so either
//                         * a flag or a group) and the current argument doesn't announce such, then the argument
//                         * is invalid and therefore an exception is thrown.
//                         */
//                        if ( candidate != null ) {
//
//                            /* Some annotated arguments might have alternatives or aliases.
//                             * They exclude each other, which means that if any alternative
//                             * can be found in the cache collection, an exception is thrown
//                             * because otherwise there would be multiple arguments that
//                             * exclude each other.
//                             */
//                            boolean excluded = candidate.getAlternatives()
//                                                        .stream()
//                                                        .anyMatch( alt -> ANNOTATED_FETCH.containsKey( alt.getIdentifier() ) );
//                            if ( !excluded ) {
//
//                                if ( candidate instanceof Group ) {
//
//                                    parent         = ( Group ) candidate;
//                                    expectedValues = parent.getSchemes().size();
//                                    values         = new ArrayList<>();
//
//                                } else ANNOTATED_FETCH.put( element, null );
//
//                            } else {
//
//                                //TODO alt groups
//                                AnnotatedArgument included = candidate.getAlternatives()
//                                                                      .stream()
//                                                                      .filter( alt -> ANNOTATED_FETCH.containsKey( alt.getIdentifier() ) )
//                                                                      .findAny().get();
//                                throw new ExcludedArgumentException( element, included.getIdentifier() );
//
//                            }
//
//                        } else throw new InvalidFlagException( element );
//
//                    } else {
//
//                        if ( candidate == null ) {
//
//                            /* If the scheme applies to the current argument, then it is a valid
//                             * argument value for @parent.
//                             * When @values has reached the size of the expected value count,
//                             * then @parent is set back to null, so that the next iteration can
//                             * start and a new group or flag can be processed.
//                             */
//                            Scheme scheme = parent.getSchemes().get( values.size() );
//                            if ( scheme.applies( element ) ) {
//
//                                values.add( element );
//                                if ( values.size() == expectedValues ) {
//
//                                    ANNOTATED_FETCH.put( parent.getIdentifier(), values );
//                                    parent = null;
//
//                                }
//
//                            } else throw new SchemeMismatchException( element );
//
//                        } else throw new TooFewArgumentValuesException( parent.getIdentifier(), values.size(), expectedValues );
//
//                    }
//
//                }
//
//                /* If parent is not null, parsing wasn't done because there were
//                 * some argument values missing.
//                 */
//                if ( parent != null ) {
//
//                    throw new TooFewArgumentValuesException( parent.getIdentifier(), values.size(), parent.getSchemes().size() );
//
//                }
//
//            }
//
//        } catch ( InvalidFlagException | ExcludedArgumentException | SchemeMismatchException | TooFewArgumentValuesException exception ) {
//
//            exception.printStackTrace();
//
//        }
//
//        /* 3. Step: Evaluating dependencies and their fulfillment.
//         * ----------------------------------------------------------
//         * Annotated arguments can have dependencies which must be evaluated.
//         * If an argument doesn't have its dependencies fulfilled, an
//         * exception is thrown.
//         */
//        try {
//
//            for ( String argument : ANNOTATED_FETCH.keySet() ) {
//
//                List< AnnotatedArgument > dependencies = ANNOTATED_RULES.get( argument ).getDependencies();
//                for ( AnnotatedArgument dependency : dependencies ) {
//
//                    String identifier = dependency.getIdentifier();
//                    if ( !ANNOTATED_FETCH.containsKey( identifier ) ) {
//
//                        throw new UnfulfilledArgumentDependencyException( argument, identifier );
//
//                    }
//
//                }
//
//            }
//
//        } catch ( UnfulfilledArgumentDependencyException exception ) {
//
//            exception.printStackTrace();
//
//        }
//
//        /* x. Step: Checking if all essential arguments are provided.
//         * ----------------------------------------------------------
//         */
//        try {
//
//            for ( Map.Entry< String, AnnotatedArgument > entry : ANNOTATED_RULES.entrySet() ) {
//
//                if ( entry.getValue().isEssential() && !ANNOTATED_FETCH.containsKey( entry.getKey() ) ) {
//
//                    throw new EssentialArgumentMissingException( entry.getKey() );
//
//                }
//
//            }
//
//        } catch ( EssentialArgumentMissingException exception ) {
//
//            exception.printStackTrace();
//
//        }

    }

//    public static String getValue( int index ) {
//
//        return Caesar.INDEXED_FETCH.get( index );
//
//    }
//
//    public static String getValue( String identifier ) {
//
//        return Caesar.ANNOTATED_FETCH.get( identifier );
//
//    }
//
//    public static boolean isPresent( String identifier ) {
//
//        return Caesar.ANNOTATED_FETCH.containsKey( identifier );
//
//    }

    public static void main( String ... arguments ) {

        List< Argument > config = new ArrayList<>();
        Flag a = new Flag( false, "-a", null, null );
        Flag b = new Flag( false, "-b", Collections.singletonList(a), null );
        Flag c = new Flag( false, "-c", Collections.singletonList(b), null );
        Flag d = new Flag( false, "-d", Collections.singletonList(b), null );

        config.add( c );
        config.add( a );
        config.add( d );
        config.add( b );

        Caesar.configure( config );
        ARG_ALIASES.forEach( ( k, aliases ) -> {

            System.out.println( "key: " + k );
            aliases.forEach( arg -> System.out.println( arg.getIdentifier() + " / " + arg.isEssential() ) );

        } );

    }

}
