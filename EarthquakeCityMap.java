package module6;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import de.fhpotsdam.unfolding.UnfoldingMap;
import de.fhpotsdam.unfolding.data.Feature;
import de.fhpotsdam.unfolding.data.GeoJSONReader;
import de.fhpotsdam.unfolding.data.PointFeature;
import de.fhpotsdam.unfolding.geo.Location;
import de.fhpotsdam.unfolding.marker.AbstractShapeMarker;
import de.fhpotsdam.unfolding.marker.Marker;
import de.fhpotsdam.unfolding.marker.MultiMarker;
import de.fhpotsdam.unfolding.providers.AbstractMapProvider;
import de.fhpotsdam.unfolding.providers.Google;
import de.fhpotsdam.unfolding.providers.MBTilesMapProvider;
import de.fhpotsdam.unfolding.providers.Microsoft;
import de.fhpotsdam.unfolding.providers.OpenStreetMap;
import de.fhpotsdam.unfolding.utils.MapUtils;
import parsing.ParseFeed;
import processing.core.PApplet;
import processing.core.PConstants;

/**
 * EarthquakeCityMap An application with an interactive map displaying
 * earthquake data. Author: UC San Diego Intermediate Software Development MOOC
 * team
 * 
 * @author Omurbaeva Munara Date: March 16, 2022
 */
public class EarthquakeCityMap extends PApplet {

	// We will use member variables, instead of local variables, to store the data
	// that the setUp and draw methods will need to access (as well as other
	// methods)
	// You will use many of these variables, but the only one you should need to add
	// code to modify is countryQuakes, where you will store the number of
	// earthquakes
	// per country.

	// You can ignore this. It's to get rid of eclipse warnings
	private static final long serialVersionUID = 1L;

	// IF YOU ARE WORKING OFFILINE, change the value of this variable to true
	private static final boolean offline = false;

	/**
	 * This is where to find the local tiles, for working without an Internet
	 * connection
	 */
	public static String mbTilesString = "blankLight-1-3.mbtiles";

	// feed with magnitude 2.5+ Earthquakes
	private String earthquakesURL = "https://earthquake.usgs.gov/earthquakes/feed/v1.0/summary/2.5_week.atom";

	// The files containing city names and info and country names and info
	private String cityFile = "city-data.json";
	private String countryFile = "countries.geo.json";

	// The map
	private UnfoldingMap map;
	private AbstractMapProvider provider;
	private AbstractMapProvider provider1;
	private AbstractMapProvider provider2;
	// Markers for each city
	private List<Marker> cityMarkers;
	// Markers for each earthquake
	private List<Marker> quakeMarkers;

	// A List of country markers
	private List<Marker> countryMarkers;

	// NEW IN MODULE 5
	private CommonMarker lastSelected;
	private CommonMarker lastClicked;

	// fields for clicking the city marker
	private int nearbyEarthquake;
	private float averageMagnitude;
	private EarthquakeMarker mostRecent;

	public void setup() {
		// (1) Initializing canvas and map tiles
		size(820, 750, OPENGL);
		if (offline) {
			map = new UnfoldingMap(this, 150, 50, 650, 600, new MBTilesMapProvider(mbTilesString));
			earthquakesURL = "2.5_week.atom"; // The same feed, but saved August 7, 2015
		} else {
			provider = new Microsoft.HybridProvider();
			provider1 = new Google.GoogleMapProvider();
			provider2 = new OpenStreetMap.OpenStreetMapProvider();
			map = new UnfoldingMap(this, 150, 50, 650, 600, provider);
			// IF YOU WANT TO TEST WITH A LOCAL FILE, uncomment the next line
			// earthquakesURL = "2.5_week.atom";
		}
		MapUtils.createDefaultEventDispatcher(this, map);

		// FOR TESTING: Set earthquakesURL to be one of the testing files by
		// uncommenting
		// one of the lines below. This will work whether you are online or offline
		// earthquakesURL = "test1.atom";
		// earthquakesURL = "test2.atom";

		// Uncomment this line to take the quiz
		 //earthquakesURL = "quiz2.atom";

		// (2) Reading in earthquake data and geometric properties
		// STEP 1: load country features and markers
		List<Feature> countries = GeoJSONReader.loadData(this, countryFile);
		countryMarkers = MapUtils.createSimpleMarkers(countries);

		// STEP 2: read in city data
		List<Feature> cities = GeoJSONReader.loadData(this, cityFile);
		cityMarkers = new ArrayList<Marker>();
		for (Feature city : cities) {
			cityMarkers.add(new CityMarker(city));
		}

		// STEP 3: read in earthquake RSS feed
		List<PointFeature> earthquakes = ParseFeed.parseEarthquake(this, earthquakesURL);
		quakeMarkers = new ArrayList<Marker>();

		for (PointFeature feature : earthquakes) {
			// check if LandQuake
			if (isLand(feature)) {
				quakeMarkers.add(new LandQuakeMarker(feature));
			}
			// OceanQuakes
			else {
				quakeMarkers.add(new OceanQuakeMarker(feature));
			}
		}

		// could be used for debugging
		printQuakes();
		sortAndPrint(20);

		// (3) Add markers to map
		// NOTE: Country markers are not added to the map. They are used
		// for their geometric properties
		map.addMarkers(quakeMarkers);
		map.addMarkers(cityMarkers);

	} // End setup

	private void showEqTitle(EarthquakeMarker m, UnfoldingMap map) {
		float x = m.getScreenPosition(map).x;
		float y = m.getScreenPosition(map).y;

		String title = m.getTitle();

		pushStyle();

		rectMode(PConstants.CORNER);
		noStroke();
		fill(255, 255, 255);
		rect(x, y + 15, textWidth(title) + 6, 18, 5);
		textAlign(PConstants.LEFT, PConstants.TOP);
		fill(0);
		text(title, x + 3, y + 18);

		popStyle();
	}

	public void draw() {
		background(255, 255, 255);
		map.draw();
		addKey();
		for (Marker m : quakeMarkers) {
			EarthquakeMarker eq = (EarthquakeMarker) m;
			if (m.isSelected()) {
				showEqTitle(eq, map);
			}
		}

		if (lastClicked instanceof CityMarker) {
			popMenu();
		}
	}

	// TODO: Add the method:
	private void sortAndPrint(int numToPrint) {
		List<EarthquakeMarker> mlist = new ArrayList<EarthquakeMarker>();
		EarthquakeMarker eqm;
		for (Marker m : quakeMarkers) {
			eqm = (EarthquakeMarker) m;
			mlist.add(eqm);
		}
		Collections.sort(mlist);
		int currnum = (numToPrint >= mlist.size() ? mlist.size() : numToPrint);
		for (int i = 0; i < currnum; i++) {
			System.out.println(mlist.get(i));
		}
	}
	// and then call that method from setUp

	/**
	 * Event handler that gets called automatically when the mouse moves.
	 */
	@Override
	public void mouseMoved() {
		// clear the last selection
		if (lastSelected != null) {
			lastSelected.setSelected(false);
			lastSelected = null;

		}
		selectMarkerIfHover(quakeMarkers);
		selectMarkerIfHover(cityMarkers);
		// loop();
	}

	// If there is a marker selected
	private void selectMarkerIfHover(List<Marker> markers) {
		// Abort if there's already a marker selected
		if (lastSelected != null) {
			return;
		}

		for (Marker m : markers) {
			CommonMarker marker = (CommonMarker) m;
			if (marker.isInside(map, mouseX, mouseY)) {
				lastSelected = marker;
				marker.setSelected(true);
				return;
			}
		}
	}

	/**
	 * The event handler for mouse clicks It will display an earthquake and its
	 * threat circle of cities Or if a city is clicked, it will display all the
	 * earthquakes where the city is in the threat circle
	 */
	@Override
	public void mouseClicked() {
		if(lastClicked != null) {
			lastClicked.setClicked(false);
			lastClicked = null;
			unhideMarkers();
			nearbyEarthquake = 0;
			averageMagnitude = 0.0f;
			mostRecent = null;			
		} else if (lastClicked == null) {
			checkMarkersForClick(quakeMarkers);
			checkMarkersForClick(cityMarkers);			
			if(lastClicked instanceof EarthquakeMarker) {
				hideOtherMarkers(quakeMarkers);
				hideCityMarkers(cityMarkers);				
			} else if (lastClicked instanceof CityMarker) {
				hideOtherMarkers(cityMarkers);
				hideQuakeMarkers(quakeMarkers);				
			} 
		}
	}

	private void showRecentEarthquakes() {
		for(Marker earthquake : quakeMarkers) {
			if(earthquake.getStringProperty("age").equals("Past Hour") || earthquake.getStringProperty("age").equals("Past Day")) {
				earthquake.setHidden(false);
			} else {
				earthquake.setHidden(true);
			}
		}
	}
	
	public void keyPressed() {
		if(key == '1') {
			map.mapDisplay.setProvider(provider);
		} else if(key == '2') {
			map.mapDisplay.setProvider(provider1);
		} else if(key == '3') {
			map.mapDisplay.setProvider(provider2);
		} else if(key == '4') {
			showRecentEarthquakes();
		} else if(key == '5') {
			unhideMarkers();
		}
	}
	
	private void hideCityMarkers(List<Marker> cities) {
		for (Marker city : cities) {
			if (city.getDistanceTo(lastClicked.getLocation()) > ((EarthquakeMarker) lastClicked).threatCircle()) {
				city.setHidden(true);
			} else {
				city.setHidden(false);
			}
		}
	}

	private void hideQuakeMarkers(List<Marker> earthquakes) {
		for (Marker earthquake : earthquakes) {
			if (earthquake.getDistanceTo(lastClicked.getLocation()) > ((EarthquakeMarker) earthquake).threatCircle()) {
				earthquake.setHidden(true);
			} else {
				earthquake.setHidden(false);
				nearbyEarthquake++;
				averageMagnitude += ((EarthquakeMarker) earthquake).getMagnitude();
				if (earthquake.getStringProperty("age").equals("Past Hour")
						|| earthquake.getStringProperty("age").equals("Past Day")) {
					mostRecent = (EarthquakeMarker) earthquake;
				}
			}
		}
	}

	private void hideOtherMarkers(List<Marker> markers) {
		for (Marker marker : markers) {
			if (marker != lastClicked) {
				marker.setHidden(true);
			}
		}
	}

	// Helper method that will check if an earthquake marker was clicked on
	// and respond appropriately
	private void checkMarkersForClick(List<Marker> markers) {
		// TODO Auto-generated method stub
		for (Marker marker : markers) {
			if (lastClicked != null) {
				break;
			}
			if (!marker.isHidden() && marker.isInside(map, mouseX, mouseY)) {
				lastClicked = (CommonMarker) marker;
				lastClicked.setClicked(true);
				break;
			}
		}
	}

	// loop over and unhide all markers
	private void unhideMarkers() {
		for (Marker marker : quakeMarkers) {
			marker.setHidden(false);
		}

		for (Marker marker : cityMarkers) {
			marker.setHidden(false);
		}
	}

	// helper method to draw key in GUI
	private void addKey() {
		// Remember you can use Processing's graphics methods here
		fill(23, 28, 43);
		noStroke();
		int xbase = 25;
		int ybase = 50;

		rect(xbase, ybase, 150, 600);

		fill(250, 250, 250);
		int tri_xbase = xbase + 35;
		int tri_ybase = ybase + 50;
		triangle(tri_xbase, tri_ybase - CityMarker.TRI_SIZE, tri_xbase - CityMarker.TRI_SIZE,
				tri_ybase + CityMarker.TRI_SIZE, tri_xbase + CityMarker.TRI_SIZE, tri_ybase + CityMarker.TRI_SIZE);

		fill(255, 255, 255);
		textSize(12);
		textAlign(LEFT, CENTER);
		text("City Quakes", tri_xbase + 15, tri_ybase);

		// extension on my own keys for markers
		float x = 90;
		float y = 250;
		textAlign(CENTER, BOTTOM);

		pushMatrix();
		translate(x, y);
		rotate(-HALF_PI);
		text("Light <------- depth --------> Deep", 0, 0);
		popMatrix();

		pushMatrix();
		noFill();
		int w = 20;
		int h = 200;
		rect(75, 150, w, h);
		for (int i = 0; i < h; i++) {
			for (int c = 255; c > 0; c--) {
				stroke(222, i, c);
				line(60, i + 135, 60, 365);
			}
		}

		popMatrix();

		text("Land Quake", xbase + 85, ybase + 362);
		text("Ocean Quake", xbase + 90, ybase + 392);
		text("Size ~ Magnitude", xbase + 80, ybase + 425);

		noFill();
		strokeWeight(1);
		stroke(255, 255, 255);
		ellipse(xbase + 35, ybase + 355, 10, 10);
		rect(xbase + 35 - 5, ybase + 380, 10, 10);
		fill(23, 28, 43);
		textSize(15);
		textAlign(LEFT, CENTER);
		text("Press 1 for default map view. Press 2 or 3 for different map views", xbase + 10, ybase + 625);
		text("Press 4 for recent earthquakes. Press 5 to unhide all markers", xbase + 10, ybase + 655);
		text("Click on city to see more information", xbase + 10, ybase + 685);

	}

	// Checks whether this quake occurred on land. If it did, it sets the
	// "country" property of its PointFeature to the country where it occurred
	// and returns true. Notice that the helper method isInCountry will
	// set this "country" property already. Otherwise it returns false.
	private boolean isLand(PointFeature earthquake) {

		// IMPLEMENT THIS: loop over all countries to check if location is in any of
		// them
		// If it is, add 1 to the entry in countryQuakes corresponding to this country.
		for (Marker country : countryMarkers) {
			if (isInCountry(earthquake, country)) {
				return true;
			}
		}

		// not inside any country
		return false;
	}

	// prints countries with number of earthquakes
	// You will want to loop through the country markers or country features
	// (either will work) and then for each country, loop through
	// the quakes to count how many occurred in that country.
	// Recall that the country markers have a "name" property,
	// And LandQuakeMarkers have a "country" property set.
	private void printQuakes() {
		int totalWaterQuakes = quakeMarkers.size();
		for (Marker country : countryMarkers) {
			String countryName = country.getStringProperty("name");
			int numQuakes = 0;
			for (Marker marker : quakeMarkers) {
				EarthquakeMarker eqMarker = (EarthquakeMarker) marker;
				if (eqMarker.isOnLand()) {
					if (countryName.equals(eqMarker.getStringProperty("country"))) {
						numQuakes++;
					}
				}
			}
			if (numQuakes > 0) {
				totalWaterQuakes -= numQuakes;
				System.out.println(countryName + ": " + numQuakes);
			}
		}
		
	}

	// helper method to test whether a given earthquake is in a given country
	// This will also add the country property to the properties of the earthquake
	// feature if
	// it's in one of the countries.
	// You should not have to modify this code
	private boolean isInCountry(PointFeature earthquake, Marker country) {
		// getting location of feature
		Location checkLoc = earthquake.getLocation();

		// some countries represented it as MultiMarker
		// looping over SimplePolygonMarkers which make them up to use isInsideByLoc
		if (country.getClass() == MultiMarker.class) {

			// looping over markers making up MultiMarker
			for (Marker marker : ((MultiMarker) country).getMarkers()) {

				// checking if inside
				if (((AbstractShapeMarker) marker).isInsideByLocation(checkLoc)) {
					earthquake.addProperty("country", country.getProperty("name"));

					// return if is inside one
					return true;
				}
			}
		}

		// check if inside country represented by SimplePolygonMarker
		else if (((AbstractShapeMarker) country).isInsideByLocation(checkLoc)) {
			earthquake.addProperty("country", country.getProperty("name"));

			return true;
		}
		return false;
	}

	// Draw a rectangle below the earthquake keys if a city marker has been clicked
	private void popMenu() {
		fill(23, 28, 43);

		int xbase = 35;
		int ybase = 480;
		noStroke();
		rect(xbase, ybase, 140, 160);

		fill(250, 250, 250);
		textAlign(LEFT, CENTER);
		textSize(12);
		text(nearbyEarthquake, xbase + 100, ybase + 20);
		textSize(12);
		text("Nearby", xbase + 17, ybase + 15);
		text("Earthquakes", xbase + 17, ybase + 30);

		text("Average", xbase + 17, ybase + 55);
		text("Magnitude", xbase + 17, ybase + 70);
		textSize(12);
		float average = averageMagnitude / nearbyEarthquake;
		if (nearbyEarthquake == 0) {
			average = 0;
		}
		text(average, xbase + 80, ybase + 60);
		textSize(12);
		text("Most Recent", xbase + 17, ybase + 105);
		text("Earthquake", xbase + 17, ybase + 120);
		if (mostRecent != null) {
			String[] title = mostRecent.getTitle().split("-");
			String magnitude = title[0].trim();
			String distance = title[1].trim().substring(0, title[1].indexOf("of") + 1);
			String loc = title[1].substring(title[1].indexOf("of") + 2).trim();
			text(magnitude, xbase + 17, ybase + 140);
			text(distance, xbase + 17, ybase + 155);
			text(loc, xbase + 17, ybase + 170);
		} else {
			textSize(12);
			text("None", xbase + 17, ybase + 140);
		}
	}

}
