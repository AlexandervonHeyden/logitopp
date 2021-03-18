package edu.kit.ifv.mobitopp.simulation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import edu.kit.ifv.mobitopp.simulation.activityschedule.randomizer.ActivityStartAndDurationRandomizer;
import edu.kit.ifv.mobitopp.simulation.destinationChoice.DestinationChoiceModel;
import edu.kit.ifv.mobitopp.simulation.events.EventQueue;
import edu.kit.ifv.mobitopp.simulation.parcels.DeliveryResults;
import edu.kit.ifv.mobitopp.simulation.parcels.Parcel;
import edu.kit.ifv.mobitopp.simulation.parcels.orders.ParcelOrderModel;
import edu.kit.ifv.mobitopp.simulation.person.DeliveryPersonFactory;
import edu.kit.ifv.mobitopp.simulation.person.PersonState;
import edu.kit.ifv.mobitopp.simulation.person.PickUpParcelPerson;
import edu.kit.ifv.mobitopp.simulation.person.PublicTransportBehaviour;
import edu.kit.ifv.mobitopp.simulation.person.TripFactory;
import edu.kit.ifv.mobitopp.simulation.tour.TourBasedModeChoiceModel;


/**
 * The Class DemandSimulatorDelivery extends the DemandSimulatorPassenger by
 * introducing parcel orders and delivery persons.
 */
public class DemandSimulatorDelivery extends DemandSimulatorPassenger {

	private final ParcelOrderModel parcelOrderModel;
	private final Collection<Parcel> parcels;
	private final DeliveryPersonFactory deliveryPersonFactory;
	private final Predicate<Person> personFilter;
	private final DeliveryResults deliveryResults;
	

	/**
	 * Instantiates a new demand simulator delivery.
	 *
	 * @param destinationChoiceModel     the destination choice model
	 * @param modeChoiceModel            the mode choice model
	 * @param routeChoice                the route choice model
	 * @param activityDurationRandomizer the activity duration randomizer
	 * @param tripFactory                the trip factory
	 * @param rescheduling               the rescheduling strategy
	 * @param modesInSimulation          the modes used in simulation
	 * @param initialState               the initial person state
	 * @param context                    the simulation context
	 * @param personFactory              the person factory
	 * @param parcelOrderModel           the parcel order model
	 * @param personFilter 				 the person filter do determine which persons should be simulated
	 */
	public DemandSimulatorDelivery(final DestinationChoiceModel destinationChoiceModel,
			final TourBasedModeChoiceModel modeChoiceModel, final ZoneBasedRouteChoice routeChoice,
			final ActivityStartAndDurationRandomizer activityDurationRandomizer,
			final TripFactory tripFactory, final ReschedulingStrategy rescheduling,
			final Set<Mode> modesInSimulation, final PersonState initialState,
			final SimulationContext context, final DeliveryPersonFactory personFactory,
			final ParcelOrderModel parcelOrderModel, final Predicate<Person> personFilter) {
		
		super(destinationChoiceModel, modeChoiceModel, routeChoice, activityDurationRandomizer,
				tripFactory, rescheduling, modesInSimulation, initialState, context,
				personFactory.getDefaultFactory());
		
		this.parcelOrderModel = parcelOrderModel;
		this.parcels = new ArrayList<Parcel>();
		this.deliveryPersonFactory = personFactory;
		this.personFilter = personFilter;
		this.deliveryResults = new DeliveryResults(context().results());

	}

	/**
	 * Initiates a fraction of households. Creates a SimulatedPerson for each person
	 * in a household. Creates parcel orders for each person in a household.
	 *
	 * @param queue             the event queue
	 * @param boarder           the public transport behavior
	 * @param seed              the seed
	 * @param listener          the person listener
	 * @param modesInSimulation the modes used in simulation
	 * @param initialState      the initial person state
	 */
	@Override
	protected void initFractionOfHouseholds(EventQueue queue, PublicTransportBehaviour boarder,
			long seed, PersonListener listener, Set<Mode> modesInSimulation,
			PersonState initialState) {
		
		Function<Person, PickUpParcelPerson> createAgent = p -> createSimulatedPerson(queue,
				boarder, seed, p, listener, modesInSimulation, initialState);
		Consumer<PickUpParcelPerson> createParcelOrders = p -> createParcelOrder(p);

		List<PickUpParcelPerson> ppps = personLoader().households().flatMap(Household::persons)
				.filter(personFilter).map(createAgent).collect(Collectors.toList());
		ppps.forEach(createParcelOrders);

		System.out.println("Generated " + this.parcels.size() + " parcels for "
				+ this.parcels.stream().map(p -> p.getPerson().getOid()).distinct().count() + "/"
				+ ppps.size() + " unique persons.");
	}

	/**
	 * Creates the simulated person.
	 *
	 * @param queue the queue
	 * @param boarder the boarder
	 * @param seed the seed
	 * @param p the p
	 * @param listener the listener
	 * @param modesInSimulation the modes in simulation
	 * @param initialState the initial state
	 * @return the pick up parcel person
	 */
	protected PickUpParcelPerson createSimulatedPerson(EventQueue queue,
			PublicTransportBehaviour boarder, long seed, Person p, PersonListener listener,
			Set<Mode> modesInSimulation, PersonState initialState) {
		return deliveryPersonFactory.create(p, queue, simulationOptions(), simulationDays(),
				modesInSimulation, tourFactory, tripFactory(), initialState, boarder, seed,
				listener, this.deliveryResults);
	}

	/**
	 * Creates the parcel orders for the given person by applying the simulator's
	 * parcelOrderModel.
	 *
	 * @param p the person
	 * @return the collection of parcels ordered by the given person
	 */
	protected Collection<Parcel> createParcelOrder(PickUpParcelPerson p) {
		Collection<Parcel> parcels = this.parcelOrderModel.createParcelOrders(p, this.deliveryResults);
		this.parcels.addAll(parcels);

		return parcels;
	}

}
