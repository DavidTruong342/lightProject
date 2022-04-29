//******************************************************************************
// Copyright (C) 2019 University of Oklahoma Board of Trustees.
//******************************************************************************
// Last modified: Wed Feb 27 17:32:08 2019 by Chris Weaver
//******************************************************************************
// Major Modification History:
//
// 20190227 [weaver]:	Original file.
//
//******************************************************************************
//
// The model manages all of the user-adjustable variables utilized in the scene.
// (You can store non-user-adjustable scene data here too, if you want.)
//
// For each variable that you want to make interactive:
//
//   1. Add a member of the right type
//   2. Initialize it to a reasonable default value in the constructor.
//   3. Add a method to access a copy of the variable's current value.
//   4. Add a method to modify the variable.
//
// Concurrency management is important because the JOGL and the Java AWT run on
// different threads. The modify methods use the GLAutoDrawable.invoke() method
// so that all changes to variables take place on the JOGL thread. Because this
// happens at the END of GLEventListener.display(), all changes will be visible
// to the View.update() and render() methods in the next animation cycle.
//
//******************************************************************************

package edu.ou.cs.cg.assignment.homework03;

import java.lang.*;
import java.awt.Point;
import java.awt.geom.Point2D;
import java.util.*;
import com.jogamp.opengl.*;
import edu.ou.cs.cg.utilities.Utilities;

//******************************************************************************

/**
 * The <CODE>Model</CODE> class.
 *
 * @author  Chris Weaver
 * @version %I%, %G%
 */
public final class Model
{
	//**********************************************************************
	// Private Members
	//**********************************************************************

	// State (internal) variables
	private final View					view;

	// Model variables - Ignore, I don't want to comment out since I don't know what might break
	private Point2D.Double				origin;	// Current origin coords
	private Point2D.Double				cursor;	// Current cursor coords
	private ArrayList<Point2D.Double>	points;	// Drawn polyline points
	private boolean					colorful;	// Show rainbow version?
	
	// Model variables
	private Point2D.Double lightPoint;
	private String status;
	private Deque<LightElement> lightElements;
	private boolean light;
	
	// Unsure if I will get rid of this
	private Deque<Point2D.Double> nodes;
	
	// I might get rid of these variables once the new object class is done
	private LightBox lightbox;
	private ArrayList<Mirror> mirrors;
	private ArrayList<Prism> prisms;
	private ArrayList<Lense> convexLenses;
	private ArrayList<Lense> concaveLenses;

	//**********************************************************************
	// Constructors and Finalizer
	//**********************************************************************

	public Model(View view)
	{
		this.view = view;

		// Initialize user-adjustable variables (with reasonable default values)
		origin = new Point2D.Double(0.0, 0.0);
		cursor = null;
		points = new ArrayList<Point2D.Double>();
		colorful = false;
		
		// Part of the light project
		lightPoint = new Point2D.Double(0.0, 0.0);
		status = "Lightbox";
		lightElements = new ArrayDeque<LightElement>();
		light = false;
		nodes = new ArrayDeque<Point2D.Double>();
		
		// May get rid of
		lightbox = new LightBox();
		mirrors = new ArrayList<Mirror>();
		prisms = new ArrayList<Prism>();
		convexLenses = new ArrayList<Lense>();
		concaveLenses = new ArrayList<Lense>();
	}

	//**********************************************************************
	// Public Methods (Access Variables)
	//**********************************************************************

	public Point2D.Double	getOrigin()
	{
		return new Point2D.Double(origin.x, origin.y);
	}

	public Point2D.Double	getCursor()
	{
		if (cursor == null)
			return null;
		else
			return new Point2D.Double(cursor.x, cursor.y);
	}

	public List<Point2D.Double>	getPolyline()
	{
		return Collections.unmodifiableList(points);
	}

	public boolean	getColorful()
	{
		return colorful;
	}
	
	// Light project get methods
	
	// Get the point of the object that represents the lightbeam
	public Point2D.Double getLightPoint()
	{
		return lightPoint;
	}
	
	public LightBox getLightBox()
	{
		return lightbox;
	}
	
	public List<Mirror> getMirrors()
	{
		return Collections.unmodifiableList(mirrors);
	}
	
	public List<Prism> getPrisms()
	{
		return Collections.unmodifiableList(prisms);
	}
	
	public List<Lense> getConvexLenses()
	{
		return Collections.unmodifiableList(convexLenses);
	}
	
	public List<Lense> getConcaveLenses()
	{
		return Collections.unmodifiableList(concaveLenses);
	}
	
	public Deque<Point2D.Double> getNodes()
	{
		return nodes;
	}
	
	public Deque<LightElement> getLightElements()
	{
		return lightElements;
	}
	
	public boolean getLight()
	{
		return light;
	}

	//**********************************************************************
	// Public Methods (Modify Variables)
	//**********************************************************************

	public void	setOriginInSceneCoordinates(Point2D.Double q)
	{
		view.getCanvas().invoke(false, new BasicUpdater() {
			public void	update(GL2 gl) {
				origin = new Point2D.Double(q.x, q.y);
			}
		});;
	}

	public void	setOriginInViewCoordinates(Point q)
	{
		view.getCanvas().invoke(false, new ViewPointUpdater(q) {
			public void	update(double[] p) {
				origin = new Point2D.Double(p[0], p[1]);
			}
		});;
	}

	public void	setCursorInViewCoordinates(Point q)
	{
		view.getCanvas().invoke(false, new ViewPointUpdater(q) {
			public void	update(double[] p) {
				cursor = new Point2D.Double(p[0], p[1]);
			}
		});;
	}

	public void	turnCursorOff()
	{
		view.getCanvas().invoke(false, new BasicUpdater() {
			public void	update(GL2 gl) {
				cursor = null;
			}
		});;
	}

	// Changed the homework method to place object depending on mode
	public void	addPolylinePointInViewCoordinates(Point q)
	{
		view.getCanvas().invoke(false, new ViewPointUpdater(q) {
			public void	update(double[] p) {
				switch(status)
				{
					case "Lightbox":
						setLightBox(q);
						break;
					case "Mirror":
						addMirror(q);
						break;
					case "Prism":
						addPrism(q);
						break;
					case "Convex":
						addLense(q, true);
						break;
					case "Concave":
						addLense(q, false);
						break;
					default:
						break;
				}
				nodes.add(new Point2D.Double(p[0], p[1]));
				//points.add(new Point2D.Double(p[0], p[1]));
			}
		});;
	}

	// Not used
	public void	clearPolyline()
	{
		view.getCanvas().invoke(false, new BasicUpdater() {
			public void	update(GL2 gl) {
				points.clear();
			}
		});;
	}

	// Not used
	public void	toggleColorful()
	{
		view.getCanvas().invoke(false, new BasicUpdater() {
			public void	update(GL2 gl) {
				colorful = !colorful;
			}
		});;
	}
	
	// Sets the type of object being places
	public void setStatus(String s)
	{
		status = new String(s);
	}
	
	// Creates lightbox or changes its location, only one lightbox should exists
	public void setLightBox(Point q)
	{
		view.getCanvas().invoke(false, new ViewPointUpdater(q) {
			public void	update(double[] p) {
				boolean noLightbox = true;
				// Old method
				lightbox = new LightBox(
									new Point2D.Double(p[0] - 25, p[1] - 25),
									new Point2D.Double(p[0] + 25, p[1] - 25),
									new Point2D.Double(p[0] + 25, p[1] + 25),
									new Point2D.Double(p[0] - 25, p[1] + 25),
									new Point2D.Double(p[0], p[1]));
				// New method
				for (LightElement le : lightElements)
				{
					if(le.getType().equals("Lightbox")) {
						noLightbox = false;
						le.setLightbox(new Point2D.Double(p[0] - 25, p[1] - 25),
									new Point2D.Double(p[0] + 25, p[1] - 25),
									new Point2D.Double(p[0] + 25, p[1] + 25),
									new Point2D.Double(p[0] - 25, p[1] + 25),
									new Point2D.Double(p[0], p[1]));
						break;
					}
				}
				if(noLightbox)
				{
					lightElements.add(new LightElement("Lightbox", 
							new Point2D.Double(p[0] - 25, p[1] - 25), 
							new Point2D.Double(p[0] + 25, p[1] - 25), 
							new Point2D.Double(p[0] + 25, p[1] + 25), 
							new Point2D.Double(p[0] - 25, p[1] + 25), 
							null, new Point2D.Double(p[0], p[1]), null, null));
				}
				//lightPoint.setLocation(p[0] + 25, p[1]);
				toggleLight(true);
			}
		});;
	}
	
	// Creates a mirror
	public void addMirror(Point q)
	{
		view.getCanvas().invoke(false, new ViewPointUpdater(q) {
			public void update(double[] p) {
				// Old method
				mirrors.add(new Mirror(
									new Point2D.Double(p[0] - 5, p[1] - 30),
									new Point2D.Double(p[0] + 5, p[1] - 30),
									new Point2D.Double(p[0] + 5, p[1] + 30),
									new Point2D.Double(p[0] - 5, p[1] + 30),
									new Point2D.Double(p[0], p[1])));
				// New method
				lightElements.add(new LightElement("Mirror", 
						new Point2D.Double(p[0] - 5, p[1] - 30), 
						new Point2D.Double(p[0] + 5, p[1] - 30), 
						new Point2D.Double(p[0] + 5, p[1] + 30),
						new Point2D.Double(p[0] - 5, p[1] + 30), 
						null, new Point2D.Double(p[0], p[1]), null, null));
				toggleLight(true);
			}
		});;
	}
	
	// Creates a prism
	public void addPrism(Point q)
	{
		view.getCanvas().invoke(false, new ViewPointUpdater(q) {
			public void update(double[] p) {
				// Old method
				prisms.add(new Prism(
									new Point2D.Double(p[0] - 25, p[1] - 25),
									new Point2D.Double(p[0] + 25, p[1] - 25),
									new Point2D.Double(p[0], p[1] + 25),
									new Point2D.Double(p[0], p[1])));
				// New method
				lightElements.add(new LightElement("Prism", 
						new Point2D.Double(p[0] - 25, p[1] - 25), 
						new Point2D.Double(p[0] + 25, p[1] - 25), 
						null, null, new Point2D.Double(p[0], p[1] + 25), 
						new Point2D.Double(p[0], p[1]), null, null));
				toggleLight(true);
			}
		});;
	}
	
	// Creates a lense (either convex or concave depending on passed boolean)
	public void addLense(Point q, boolean convex)
	{
		view.getCanvas().invoke(false, new ViewPointUpdater(q) {
			public void update(double[] p) {
				// Check if convex or concave
				if(convex) {
					// Old method
					convexLenses.add(new Lense(
							new Point2D.Double(p[0] - 5, p[1] - 30),
							new Point2D.Double(p[0] + 5, p[1] - 30),
							new Point2D.Double(p[0] + 5, p[1] + 30),
							new Point2D.Double(p[0] - 5, p[1] + 30),
							new Point2D.Double(p[0], p[1]),
							new Point2D.Double(p[0] - 15, p[1]),
							new Point2D.Double(p[0] + 15, p[1])));
					// New method
					lightElements.add(new LightElement("Convex", 
							new Point2D.Double(p[0] - 5, p[1] - 30), 
							new Point2D.Double(p[0] + 5, p[1] - 30), 
							new Point2D.Double(p[0] + 5, p[1] + 30),
							new Point2D.Double(p[0] - 5, p[1] + 30), 
							null, new Point2D.Double(p[0], p[1]), 
							new Point2D.Double(p[0] - 15, p[1]), 
							new Point2D.Double(p[0] + 15, p[1])));
				}
				else {
					// Old method
					concaveLenses.add(new Lense(
							new Point2D.Double(p[0] - 10, p[1] - 30),
							new Point2D.Double(p[0] + 10, p[1] - 30),
							new Point2D.Double(p[0] + 10, p[1] + 30),
							new Point2D.Double(p[0] - 10, p[1] + 30),
							new Point2D.Double(p[0], p[1])));
					// New method
					lightElements.add(new LightElement("Concave", 
							new Point2D.Double(p[0] - 10, p[1] - 30),
							new Point2D.Double(p[0] + 10, p[1] - 30),
							new Point2D.Double(p[0] + 10, p[1] + 30),
							new Point2D.Double(p[0] - 10, p[1] + 30),
							null, new Point2D.Double(p[0], p[1]), null, null));
				}
				toggleLight(true);
			}
		});;
	}
	
	/*
	public void addConcave(Point q)
	{
		view.getCanvas().invoke(false, new ViewPointUpdater(q) {
			public void update(double[] p) {
				concaveLenses.add(new Concave(
										new Point2D.Double(p[0] - 10, p[1] - 30),
										new Point2D.Double(p[0] + 10, p[1] - 30),
										new Point2D.Double(p[0] + 10, p[1] + 30),
										new Point2D.Double(p[0] - 10, p[1] + 30),
										new Point2D.Double(p[0], p[1])));
			}
		});
	}
	*/
	
	// Toggles drawing the light (if object is placed light is turned off, otherwise toggle)
	public void toggleLight(boolean override)
	{
		// Checks if object is placed
		if(override) {
			light = false;
		}
		else {
			light = !light;
		}
		
		// Sets the lightpoint back to the lightbox and clear the trace
		lightPoint.setLocation(lightbox.getCenter().x + 25, lightbox.getCenter().y);
		view.clearLight();
	}

	//**********************************************************************
	// Inner Classes
	//**********************************************************************

	// Convenience class to simplify the implementation of most updaters.
	private abstract class BasicUpdater implements GLRunnable
	{
		public final boolean	run(GLAutoDrawable drawable)
		{
			GL2	gl = drawable.getGL().getGL2();

			update(gl);

			return true;	// Let animator take care of updating the display
		}

		public abstract void	update(GL2 gl);
	}

	// Convenience class to simplify updates in cases in which the input is a
	// single point in view coordinates (integers/pixels).
	private abstract class ViewPointUpdater extends BasicUpdater
	{
		private final Point	q;

		public ViewPointUpdater(Point q)
		{
			this.q = q;
		}

		public final void	update(GL2 gl)
		{
			int		h = view.getHeight();
			double[]	p = Utilities.mapViewToScene(gl, q.x, h - q.y, 0.0);

			update(p);
		}

		public abstract void	update(double[] p);
	}
	
	//**********************************************************************
	// Object Classes
	//**********************************************************************
	public class LightBox {
		Point2D.Double bl;
		Point2D.Double br;
		Point2D.Double tr;
		Point2D.Double tl;
		Point2D.Double center;
		double rotation;
		
		public LightBox() {
			bl = new Point2D.Double(0.0, 0.0);
			br = new Point2D.Double(0.0, 0.0);
			tr = new Point2D.Double(0.0, 0.0);
			tl = new Point2D.Double(0.0, 0.0);
			center = new Point2D.Double(0.0, 0.0);
			rotation = 0.0;
		}
		
		public LightBox(Point2D.Double bl, Point2D.Double br, Point2D.Double tr, 
							Point2D.Double tl, Point2D.Double center) {
			this.bl = bl;
			this.br = br;
			this.tr = tr;
			this.tl = tl;
			this.center = center;
			rotation = 0.0;
		}
		
		public Point2D.Double getBl() {
			return bl;
		}
		
		public Point2D.Double getBr() {
			return br;
		}
		
		public Point2D.Double getTr() {
			return tr;
		}
		
		public Point2D.Double getTl() {
			return tl;
		}
		
		public Point2D.Double getCenter() {
			return center;
		}
		
		public double getRotation() {
			return rotation;
		}
		
		public void setRotation(double rotation) {
			this.rotation += rotation;
		}
	}
	
	public class Mirror {
		Point2D.Double bl;
		Point2D.Double br;
		Point2D.Double tr;
		Point2D.Double tl;
		Point2D.Double center;
		double rotation;
		
		public Mirror(Point2D.Double bl, Point2D.Double br, Point2D.Double tr, 
						Point2D.Double tl, Point2D.Double center) {
			this.bl = bl;
			this.br = br;
			this.tr = tr;
			this.tl = tl;
			this.center = center;
			rotation = 0.0;
		}
		
		public Point2D.Double getBl() {
			return bl;
		}
		
		public Point2D.Double getBr() {
			return br;
		}
		
		public Point2D.Double getTr() {
			return tr;
		}
		
		public Point2D.Double getTl() {
			return tl;
		}
		
		public Point2D.Double getCenter() {
			return center;
		}
		
		public double getRotation() {
			return rotation;
		}
		
		public void setRotation(double rotation) {
			this.rotation += rotation;
		}
	}
	
	public class Prism {
		Point2D.Double bl;
		Point2D.Double br;
		Point2D.Double t;
		Point2D.Double center;
		double rotation;
		
		public Prism(Point2D.Double bl, Point2D.Double br, Point2D.Double t, Point2D.Double center) {
			this.bl = bl;
			this.br = br;
			this.t = t;
			this.center = center;
			rotation = 0.0;
		}
		
		public Point2D.Double getBl() {
			return bl;
		}
		
		public Point2D.Double getBr() {
			return br;
		}
		
		public Point2D.Double getT() {
			return t;
		}
		
		public Point2D.Double getCenter() {
			return center;
		}
		
		public double getRotation() {
			return rotation;
		}
		
		public void setRotation(double rotation) {
			this.rotation += rotation;
		}
	}
	
	public class Lense {
		Point2D.Double bl;
		Point2D.Double br;
		Point2D.Double tr;
		Point2D.Double tl;
		Point2D.Double center;
		double[] rCurveX;
		double[] rCurveY;
		double[] lCurveX;
		double[] lCurveY;
		double rotation;
		
		public Lense(Point2D.Double bl, Point2D.Double br, Point2D.Double tr,
						Point2D.Double tl, Point2D.Double center, Point2D.Double leftCtrl,
						Point2D.Double rightCtrl) {
			this.bl = bl;
			this.br = br;
			this.tr = tr;
			this.tl = tl;
			this.center = center;
			rotation = 0.0;
			
			createConvex(leftCtrl, rightCtrl);
		}
		
		public Lense(Point2D.Double bl, Point2D.Double br, Point2D.Double tr,
				Point2D.Double tl, Point2D.Double center) {
			this.bl = bl;
			this.br = br;
			this.tr = tr;
			this.tl = tl;
			this.center = center;
			
			createConcave();
		}
		
		public Point2D.Double getBl() {
			return bl;
		}
		
		public Point2D.Double getBr() {
			return br;
		}
		
		public Point2D.Double getTr() {
			return tr;
		}
		
		public Point2D.Double getTl() {
			return tl;
		}
		
		public Point2D.Double getCenter() {
			return center;
		}
		
		public double[] getRCurveX() {
			return rCurveX;
		}
		
		public double[] getRCurveY() {
			return rCurveY;
		}
		
		public double[] getLCurveX() {
			return lCurveX;
		}
		
		public double[] getLCurveY() {
			return lCurveY;
		}
		
		public double getRotation() {
			return rotation;
		}
		
		public void setRotation(double rotation) {
			this.rotation += rotation;
		}
		
		private void createConvex(Point2D.Double leftCtrl, Point2D.Double rightCtrl) {
			int i;
			double t;
			
			rCurveX = new double[11];
			rCurveY = new double[11];
			lCurveX = new double[11];
			lCurveY = new double[11];
			
			for(i = 0, t = 0; i < 11 && t < 1.1; i++, t = t + 0.1) {
				rCurveX[i] = (Math.pow((1-t), 2)*br.x + 2*t*(1-t)*rightCtrl.x + Math.pow(t, 2)*tr.x);
				rCurveY[i] = (Math.pow((1-t), 2)*br.y + 2*t*(1-t)*rightCtrl.y + Math.pow(t, 2)*tr.y);
				lCurveX[i] = (Math.pow((1-t), 2)*tl.x + 2*t*(1-t)*leftCtrl.x + Math.pow(t, 2)*bl.x);
				lCurveY[i] = (Math.pow((1-t), 2)*tl.y + 2*t*(1-t)*leftCtrl.y + Math.pow(t, 2)*bl.y);
			}
		}
		
		private void createConcave() {
			int i;
			double t;
			
			rCurveX = new double[11];
			rCurveY = new double[11];
			lCurveX = new double[11];
			lCurveY = new double[11];
			
			for(i = 0, t = 0; i < 11 && t < 1.1; i++, t = t + 0.1) {
				rCurveX[i] = (Math.pow((1-t), 2)*br.x + 2*t*(1-t)*center.x + Math.pow(t, 2)*tr.x);
				rCurveY[i] = (Math.pow((1-t), 2)*br.y + 2*t*(1-t)*center.y + Math.pow(t, 2)*tr.y);
				lCurveX[i] = (Math.pow((1-t), 2)*tl.x + 2*t*(1-t)*center.x + Math.pow(t, 2)*bl.x);
				lCurveY[i] = (Math.pow((1-t), 2)*tl.y + 2*t*(1-t)*center.y + Math.pow(t, 2)*bl.y);
			}
		}
	}
	
	// This is the general method used for the new method, sorry its messy
	public class LightElement {
		Point2D.Double bl;
		Point2D.Double br;
		Point2D.Double tr;
		Point2D.Double tl;
		Point2D.Double t;
		Point2D.Double center;
		double[] rCurveX;
		double[] rCurveY;
		double[] lCurveX;
		double[] lCurveY;
		double rotation;
		String type;
		
		public LightElement(String type, Point2D.Double bl, Point2D.Double br, Point2D.Double tr,
						Point2D.Double tl, Point2D.Double t, Point2D.Double center, 
						Point2D.Double leftCtrl, Point2D.Double rightCtrl)
		{
			switch(type)
			{
				case "Lightbox":
					this.bl = bl;
					this.br = br;
					this.tr = tr;
					this.tl = tl;
					this.center = center;
					rotation = 0.0;
					this.type = new String(type);
					break;
				case "Mirror":
					this.bl = bl;
					this.br = br;
					this.tr = tr;
					this.tl = tl;
					this.center = center;
					rotation = 0.0;
					this.type = new String(type);
					break;
				case "Prism":
					this.bl = bl;
					this.br = br;
					this.t = t;
					this.center = center;
					rotation = 0.0;
					this.type = new String(type);
					break;
				case "Convex":
					this.bl = bl;
					this.br = br;
					this.tr = tr;
					this.tl = tl;
					this.center = center;
					rotation = 0.0;
					this.type = new String(type);
					
					createConvex(leftCtrl, rightCtrl);
					break;
				case "Concave":
					this.bl = bl;
					this.br = br;
					this.tr = tr;
					this.tl = tl;
					this.center = center;
					rotation = 0.0;
					this.type = new String(type);
					
					createConcave();
					break;
			}
		}
		
		// Get the type of the element
		public String getType() {
			return type;
		}
		
		// Get bottom left point
		public Point2D.Double getBl() {
			return bl;
		}
		
		// Get bottom right point
		public Point2D.Double getBr() {
			return br;
		}
		
		// Get top right point
		public Point2D.Double getTr() {
			return tr;
		}
		
		// Get top left point
		public Point2D.Double getTl() {
			return tl;
		}
		
		// Get top point (for the prism)
		public Point2D.Double getT() {
			return t;
		}
		
		// Get the center point
		public Point2D.Double getCenter() {
			return center;
		}
		
		// Get x's of the right curve for the lenses
		public double[] getRCurveX() {
			return rCurveX;
		}
		
		// Get y's of the right curve for the lenses
		public double[] getRCurveY() {
			return rCurveY;
		}
		
		// Get x's of the left curve for the lenses
		public double[] getLCurveX() {
			return lCurveX;
		}
		
		// Get y's of the left curve for the lenses
		public double[] getLCurveY() {
			return lCurveY;
		}
		
		// Get rotation of the element
		public double getRotation() {
			return rotation;
		}
		
		// Set the rotation of the element
		public void setRotation(double rotation) {
			this.rotation += rotation;
		}
		
		// Sets the location of the lightbox element
		public void setLightbox(Point2D.Double bl, Point2D.Double br, Point2D.Double tr, 
				Point2D.Double tl, Point2D.Double center) 
		{
			this.bl = bl;
			this.br = br;
			this.tr = tr;
			this.tl = tl;
			this.center = center;
			rotation = 0.0;
		}
		
		// Creates the curves for the convex lens
		private void createConvex(Point2D.Double leftCtrl, Point2D.Double rightCtrl) {
			int i;
			double t;
			
			rCurveX = new double[11];
			rCurveY = new double[11];
			lCurveX = new double[11];
			lCurveY = new double[11];
			
			for(i = 0, t = 0; i < 11 && t < 1.1; i++, t = t + 0.1) {
				rCurveX[i] = (Math.pow((1-t), 2)*br.x + 2*t*(1-t)*rightCtrl.x + Math.pow(t, 2)*tr.x);
				rCurveY[i] = (Math.pow((1-t), 2)*br.y + 2*t*(1-t)*rightCtrl.y + Math.pow(t, 2)*tr.y);
				lCurveX[i] = (Math.pow((1-t), 2)*tl.x + 2*t*(1-t)*leftCtrl.x + Math.pow(t, 2)*bl.x);
				lCurveY[i] = (Math.pow((1-t), 2)*tl.y + 2*t*(1-t)*leftCtrl.y + Math.pow(t, 2)*bl.y);
			}
		}
		
		// Creates the curves for the concave lens
		private void createConcave() {
			int i;
			double t;
			
			rCurveX = new double[11];
			rCurveY = new double[11];
			lCurveX = new double[11];
			lCurveY = new double[11];
			
			for(i = 0, t = 0; i < 11 && t < 1.1; i++, t = t + 0.1) {
				rCurveX[i] = (Math.pow((1-t), 2)*br.x + 2*t*(1-t)*center.x + Math.pow(t, 2)*tr.x);
				rCurveY[i] = (Math.pow((1-t), 2)*br.y + 2*t*(1-t)*center.y + Math.pow(t, 2)*tr.y);
				lCurveX[i] = (Math.pow((1-t), 2)*tl.x + 2*t*(1-t)*center.x + Math.pow(t, 2)*bl.x);
				lCurveY[i] = (Math.pow((1-t), 2)*tl.y + 2*t*(1-t)*center.y + Math.pow(t, 2)*bl.y);
			}
		}
	}
}

//******************************************************************************
