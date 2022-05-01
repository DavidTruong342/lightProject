//******************************************************************************
// Copyright (C) 2016-2022 University of Oklahoma Board of Trustees.
//******************************************************************************
// Last modified: Fri Feb 25 23:46:18 2022 by Chris Weaver
//******************************************************************************
// Major Modification History:
//
// 20160209 [weaver]:	Original file.
// 20190203 [weaver]:	Updated to JOGL 2.3.2 and cleaned up.
// 20190227 [weaver]:	Updated to use model and asynchronous event handling.
// 20220225 [weaver]:	Added point smoothing for Hi-DPI displays.
//
//******************************************************************************
// Notes:
//
//******************************************************************************

package edu.ou.cs.cg.assignment.lightProject;

//import java.lang.*;
import java.awt.*;
import java.awt.geom.Point2D;
import java.text.DecimalFormat;
import java.util.*;
import com.jogamp.opengl.*;
import com.jogamp.opengl.awt.GLJPanel;
import com.jogamp.opengl.glu.*;
import com.jogamp.opengl.util.FPSAnimator;
import com.jogamp.opengl.util.awt.TextRenderer;
import com.jogamp.opengl.util.gl2.GLUT;
import edu.ou.cs.cg.utilities.Utilities;

//******************************************************************************

/**
 * The <CODE>View</CODE> class.<P>
 *
 * @author  Chris Weaver
 * @version %I%, %G%
 */
public final class View
	implements GLEventListener
{
	//**********************************************************************
	// Private Class Members
	//**********************************************************************

	private static final int			DEFAULT_FRAMES_PER_SECOND = 60;
	private static final DecimalFormat	FORMAT = new DecimalFormat("0.000");

	//**********************************************************************
	// Public Class Members
	//**********************************************************************

	public static final GLUT			MYGLUT = new GLUT();
	public static final Random			RANDOM = new Random();

	//**********************************************************************
	// Private Members
	//**********************************************************************

	// State (internal) variables
	private final GLJPanel				canvas;
	private int						w;			// Canvas width
	private int						h;			// Canvas height

	private TextRenderer				renderer;

	private final FPSAnimator			animator;
	private int						counter;	// Frame counter

	private final Model				model;

	private final KeyHandler			keyHandler;
	private final MouseHandler			mouseHandler;
	
	// These are the trace for the lightbeam
	private final Deque<Point2D.Double>	hitPoints1;
	private final Deque<Point2D.Double> hitPoints2;
	
	// The vectors for the lightpoints
	private Point2D.Double vector1 = new Point2D.Double(1.0 / DEFAULT_FRAMES_PER_SECOND, 0.0);
	
	private Point2D.Double vector2 = new Point2D.Double(1.0 / DEFAULT_FRAMES_PER_SECOND, 0.0);
	
	

	//**********************************************************************
	// Constructors and Finalizer
	//**********************************************************************

	public View(GLJPanel canvas)
	{
		this.canvas = canvas;

		// Initialize rendering
		counter = 0;
		canvas.addGLEventListener(this);

		// Initialize model (scene data and parameter manager)
		model = new Model(this);
		
		hitPoints1 = new ArrayDeque<Point2D.Double>();
		hitPoints2 = new ArrayDeque<Point2D.Double>();

		// Initialize controller (interaction handlers)
		keyHandler = new KeyHandler(this, model);
		mouseHandler = new MouseHandler(this, model);

		// Initialize animation
		animator = new FPSAnimator(canvas, DEFAULT_FRAMES_PER_SECOND);
		animator.start();
	}

	//**********************************************************************
	// Getters and Setters
	//**********************************************************************

	public GLJPanel	getCanvas()
	{
		return canvas;
	}

	public int	getWidth()
	{
		return w;
	}

	public int	getHeight()
	{
		return h;
	}
	
	//**********************************************************************
	// Public methods
	//**********************************************************************
	
	// Clears the trace and resets the vector
	public void clearLight()
	{
		hitPoints1.clear();
		
		ArrayDeque<Model.LightElement> elements = new ArrayDeque<Model.LightElement>(model.getLightElements());
		
		boolean noLightBox = true;
		for(Model.LightElement le : elements)
		{
			if(le.getType().equals("Lightbox"))
			{
				double rotation = le.getRotation();
				
				// Get transformed points
				Point2D.Double tr = getTransformedPoint(le.getTr(), le);
				Point2D.Double br = getTransformedPoint(le.getBr(), le);
				
				// Calculate the CW (outward-pointing) perp vector for the side.
				// Calc side vector from lightbox bottom right to top right
				double vdx = tr.x - br.x;
				double vdy = tr.y - br.y;
				
				double		ndx = vdy;				// Calc perp vector:
				double		ndy = -vdx;				// negate x and swap

				// Need a NORMALIZED perp vector
				double		nn = Math.sqrt(ndx * ndx + ndy * ndy);

				ndx = ndx / nn;	// Divide each coordinate by the length to
				ndy = ndy / nn;	// make a UNIT vector normal to the side.
				
				vector1.setLocation(ndx / DEFAULT_FRAMES_PER_SECOND, ndy / DEFAULT_FRAMES_PER_SECOND);
				noLightBox = false;
				break;
			}
		}
		
		if(noLightBox)
			vector1.setLocation(1.0 / DEFAULT_FRAMES_PER_SECOND, 0.0);
	}

	//**********************************************************************
	// Override Methods (GLEventListener)
	//**********************************************************************

	public void	init(GLAutoDrawable drawable)
	{
		w = drawable.getSurfaceWidth();
		h = drawable.getSurfaceHeight();

		renderer = new TextRenderer(new Font("Monospaced", Font.PLAIN, 12),
									true, true);

		initPipeline(drawable);
	}

	public void	dispose(GLAutoDrawable drawable)
	{
		renderer = null;
	}

	public void	display(GLAutoDrawable drawable)
	{
		updatePipeline(drawable);

		update(drawable);
		render(drawable);
	}

	public void	reshape(GLAutoDrawable drawable, int x, int y, int w, int h)
	{
		this.w = w;
		this.h = h;
	}

	//**********************************************************************
	// Private Methods (Rendering)
	//**********************************************************************

	private void	update(GLAutoDrawable drawable)
	{
		counter++;									// Advance animation counter
		
		Point2D.Double lp = model.getLightPoint();
		
		updatePointWithReflection(lp);
	}

	private void	render(GLAutoDrawable drawable)
	{
		GL2	gl = drawable.getGL().getGL2();

		gl.glClear(GL.GL_COLOR_BUFFER_BIT);		// Clear the buffer

		// Draw the scene
		drawMain(gl);								// Draw main content
		drawMode(drawable);						// Draw mode text

		gl.glFlush();								// Finish and display
	}

	//**********************************************************************
	// Private Methods (Pipeline)
	//**********************************************************************

	private void	initPipeline(GLAutoDrawable drawable)
	{
		GL2	gl = drawable.getGL().getGL2();

		gl.glClearColor(0.306f, 0.306f, 0.306f, 0.0f);	// Black background

		// Make points easier to see on Hi-DPI displays
		gl.glEnable(GL2.GL_POINT_SMOOTH);	// Turn on point anti-aliasing
	}

	private void	updatePipeline(GLAutoDrawable drawable)
	{
		GL2			gl = drawable.getGL().getGL2();
		GLU			glu = GLU.createGLU();

		gl.glMatrixMode(GL2.GL_PROJECTION);		// Prepare for matrix xform
		gl.glLoadIdentity();						// Set to identity matrix
		glu.gluOrtho2D(0, 1280, 0, 720);	// 2D translate and scale
	}

	//**********************************************************************
	// Private Methods (Scene)
	//**********************************************************************

	private void	drawMode(GLAutoDrawable drawable)
	{
		GL2		gl = drawable.getGL().getGL2();
		double[]	p = Utilities.mapViewToScene(gl, 0.5 * w, 0.5 * h, 0.0);
		double[]	q = Utilities.mapSceneToView(gl, 0.0, 0.0, 0.0);
		String drawingMode = ("Currently placing: [" + model.getStatus() + "]");

		renderer.beginRendering(w, h);

		// Draw all text in yellow
		renderer.setColor(1.0f, 1.0f, 0.0f, 1.0f);

		Point2D.Double	cursor = model.getCursor();

		if (cursor != null)
		{
			String		sx = FORMAT.format(new Double(cursor.x));
			String		sy = FORMAT.format(new Double(cursor.y));
			String		s = "Pointer at (" + sx + "," + sy + ")";

			renderer.draw(s, 2, 2);
		}
		else
		{
			renderer.draw("No Pointer", 2, 2);
		}

		renderer.draw(drawingMode, 2, 16);

		renderer.endRendering();
	}

	private void	drawMain(GL2 gl)
	{
		drawCursor(gl);							// Crosshairs at mouse point
		
		// Light project draw methods
		drawLight(gl);			// Draw the light beam
		drawLightBox(gl);		// Draw the lightbox
		drawMirrors(gl);		// Draw the mirrors
		drawPrisms(gl);			// Draw the prisms
		drawConvex(gl);			// Draw convex lenses
		drawConcave(gl);		// Draw concave lenses
		
		// Debugging method that draws the lightpoint
		drawObject(gl);
	}
	
	// Debugging method to make the location of the lightpoint known
	private void	drawObject(GL2 gl)
	{
		Point2D.Double	object = model.getLightPoint();

		gl.glColor3f(1.0f, 0.0f, 0.0f);

		gl.glBegin(GL.GL_POINTS);

		gl.glVertex2d(object.x, object.y);

		gl.glEnd();
	}

	// Draw a cursor
	private void	drawCursor(GL2 gl)
	{
		Point2D.Double	cursor = model.getCursor();

		if (cursor == null)
			return;

		gl.glBegin(GL.GL_LINE_LOOP);
		gl.glColor3f(0.5f, 0.5f, 0.5f);

		for (int i=0; i<32; i++)
		{
			double	theta = (2.0 * Math.PI) * (i / 32.0);

			gl.glVertex2d(cursor.x + 0.05 * Math.cos(theta),
						  cursor.y + 0.05 * Math.sin(theta));
		}

		gl.glEnd();
	}
	
	// Draw the lightbox
	private void drawLightBox(GL2 gl)
	{
		ArrayDeque<Model.LightElement> elements = new ArrayDeque<Model.LightElement>(model.getLightElements());
		
		setColor(gl, 93, 201, 244);		// Cyan
		
		
		
		for(Model.LightElement le : elements)
		{
			if(le.getType().equals("Lightbox"))
			{
				gl.glPushMatrix();
				
				gl.glTranslated(le.getCenter().x, le.getCenter().y, 0.0);
				gl.glRotated(le.getRotation(), 0.0, 0.0, 1.0);
				
				gl.glBegin(GL2.GL_POLYGON);
				
				gl.glVertex2d(-25.0, -25.0);
				gl.glVertex2d(25.0, -25.0);
				gl.glVertex2d(25.0, 25.0);
				gl.glVertex2d(-25.0, 25.0);
				
				gl.glEnd();
				
				gl.glPopMatrix();
				
				break;
			}
		}
		
	}
	
	// Draw the lightbeam
	private void drawLight(GL2 gl)
	{
		if(!model.getLight()) {
			return;
		}
		
		gl.glColor3f(1.0f, 1.0f, 1.0f);
		
		gl.glBegin(GL.GL_LINE_STRIP);
		
		for (Point2D.Double hp : hitPoints1)
		{
			gl.glVertex2d(hp.x, hp.y);
		}
		
		gl.glEnd();
	}
	
	// Draw the mirrors
	private void drawMirrors(GL2 gl)
	{
		ArrayDeque<Model.LightElement> elements = new ArrayDeque<Model.LightElement>(model.getLightElements());
		
		setColor(gl, 199, 199, 199);	// Light gray
		
		for(Model.LightElement le : elements)
		{
			if(le.getType().equals("Mirror"))
			{
				gl.glPushMatrix();
				
				gl.glTranslated(le.getCenter().x, le.getCenter().y, 0.0);
				gl.glRotated(le.getRotation(), 0.0, 0.0, 1.0);
				
				gl.glBegin(GL2.GL_POLYGON);

				gl.glVertex2d(-5.0, -30.0);
				gl.glVertex2d(5.0, -30.0);
				gl.glVertex2d(5.0, 30.0);
				gl.glVertex2d(-5.0, 30.0);

				gl.glEnd();
				
				gl.glPopMatrix();
			}
		}
	}
	
	// Draw the prisms
	private void drawPrisms(GL2 gl)
	{
		ArrayDeque<Model.LightElement> elements = new ArrayDeque<Model.LightElement>(model.getLightElements());
		
		setColor(gl, 199, 199, 199);	// Light gray
		
		for(Model.LightElement le : elements)
		{
			if(le.getType().equals("Prism"))
			{
				gl.glPushMatrix();
				
				gl.glTranslated(le.getCenter().x, le.getCenter().y, 0.0);
				gl.glRotated(le.getRotation(), 0.0, 0.0, 1.0);
				
				gl.glBegin(GL2.GL_POLYGON);
				
				gl.glVertex2d(-25.0, -25.0);
				gl.glVertex2d(25.0, -25.0);
				gl.glVertex2d(0.0, 25.0);
				
				gl.glEnd();
				
				gl.glPopMatrix();
			}
		}
	}
	
	// Draw the convex lenses
	private void drawConvex(GL2 gl)
	{
		ArrayDeque<Model.LightElement> elements = new ArrayDeque<Model.LightElement>(model.getLightElements());
		
		setColor(gl, 199, 199, 199);	// Light gray
		
		int i;
		double[] rCurveX;
		double[] rCurveY;
		double[] lCurveX;
		double[] lCurveY;
		
		for(Model.LightElement le : elements)
		{
			if(le.getType().equals("Convex"))
			{
				rCurveX = le.getDefaultRCurveX();
				rCurveY = le.getDefaultRCurveY();
				lCurveX = le.getDefaultLCurveX();
				lCurveY = le.getDefaultLCurveY();
				
				gl.glPushMatrix();
				
				gl.glTranslated(le.getCenter().x, le.getCenter().y, 0.0);
				gl.glRotated(le.getRotation(), 0.0, 0.0, 1.0);
				
				gl.glBegin(GL2.GL_POLYGON);
			
				gl.glVertex2d(-5.0, -30.0);
				gl.glVertex2d(5.0, -30.0);
				
				for(i = 0; i < 11; i++)
				{
					gl.glVertex2d(rCurveX[i], rCurveY[i]);
				}
				
				gl.glVertex2d(5.0, 30.0);
				gl.glVertex2d(-5.0, 30.0);
				
				for(i = 0; i < 11; i++)
				{
					gl.glVertex2d(lCurveX[i], lCurveY[i]);
				}
				
				gl.glEnd();
				
				gl.glPopMatrix();
			}
		}
	}
	
	// Draw the concave lenses
	private void drawConcave(GL2 gl)
	{
		ArrayDeque<Model.LightElement> elements = new ArrayDeque<Model.LightElement>(model.getLightElements());
		
		int i;
		double[] rCurveX;
		double[] rCurveY;
		double[] lCurveX;
		double[] lCurveY;
		
		setColor(gl, 199, 199, 199);	// Light gray
		
		for(Model.LightElement le : elements)
		{
			if(le.getType().equals("Concave"))
			{
				rCurveX = le.getDefaultRCurveX();
				rCurveY = le.getDefaultRCurveY();
				lCurveX = le.getDefaultLCurveX();
				lCurveY = le.getDefaultLCurveY();
				
				gl.glPushMatrix();
				
				gl.glTranslated(le.getCenter().x, le.getCenter().y, 0.0);
				gl.glRotated(le.getRotation(), 0.0, 0.0, 1.0);
				
				gl.glBegin(GL2.GL_TRIANGLE_FAN);
				
				gl.glVertex2d(0.0, 0.0);
				
				gl.glVertex2d(-10.0, -30.0);
				gl.glVertex2d(10.0, -30.0);
				
				for(i = 0; i < 11; i++)
				{
					gl.glVertex2d(rCurveX[i], rCurveY[i]);
				}
				
				gl.glVertex2d(10.0, 30.0);
				gl.glVertex2d(-10.0, 30.0);
				
				for(i = 0; i < 11; i++)
				{
					gl.glVertex2d(lCurveX[i], lCurveY[i]);
				}
				
				gl.glEnd();
				
				gl.glPopMatrix();
			}
		}
	}
	
	// Update the location of the light point
	private void updatePointWithReflection(Point2D.Double lp)
	{
		// Check if a lightbox has been set
		if((lp.x == 0.0 && lp.y == 0.0) || !model.getLight())
		{
			return;
		}
		
		// Travel factor and factored direction vector
		double factor = w * 0.1;
		double ddx = vector1.x * factor;
		double ddy = vector1.y * factor;
		
		System.out.println(factor);
		
		// Get the list of objects in scene
		ArrayDeque<Model.LightElement> elements = new ArrayDeque<Model.LightElement>(model.getLightElements());
		
		// Method variables
		int number = elements.size();
		int i;
		String hitType = "NA";
		
		pointCalc: while (true)
		{
			// Calculate which side the point will reach first. These variables
			// store that side's vertices and the parametric time to hit it.
			Point2D.Double		pp1 = new Point2D.Double();
			Point2D.Double		pp2 = new Point2D.Double();
			double				tmin = Double.MAX_VALUE;
			
			// Temp used to store previous tmin value
			double 				temp;
			
			// Adds a point to the trace if there are none
			if(hitPoints1.isEmpty())
			{
				hitPoints1.add(new Point2D.Double(lp.x, lp.y));
			}
			
			// Get the tmins of all objects
			for(i = 0; i < number; i++)
			{
				Model.LightElement element = elements.peekFirst();
				temp = tmin;
				
				switch(element.getType()) 
				{
					// The lenses should check the curve, but for now they only use the corners
					case "Convex":
					case "Concave":
					case "Mirror":
					case "Lightbox":
						tmin = getTMin(getTransformedPoint(element.getBl(), element), 
										getTransformedPoint(element.getBr(), element), 
										ddx, ddy, pp1, pp2, tmin, lp);
						tmin = getTMin(getTransformedPoint(element.getBr(), element), 
										getTransformedPoint(element.getTr(), element), 
										ddx, ddy, pp1, pp2, tmin, lp);
						tmin = getTMin(getTransformedPoint(element.getTr(), element), 
										getTransformedPoint(element.getTl(), element), 
										ddx, ddy, pp1, pp2, tmin, lp);
						tmin = getTMin(getTransformedPoint(element.getTl(), element), 
										getTransformedPoint(element.getBl(), element), 
										ddx, ddy, pp1, pp2, tmin, lp);
						break;
					case "Prism":
						tmin = getTMin(getTransformedPoint(element.getBl(), element), 
										getTransformedPoint(element.getBr(), element), 
										ddx, ddy, pp1, pp2, tmin, lp);
						tmin = getTMin(getTransformedPoint(element.getBr(), element), 
										getTransformedPoint(element.getT(), element), 
										ddx, ddy, pp1, pp2, tmin, lp);
						tmin = getTMin(getTransformedPoint(element.getT(), element), 
										getTransformedPoint(element.getBl(), element), 
										ddx, ddy, pp1, pp2, tmin, lp);
						break;
				}
				
				// Sets the hit type to the lowest tmin object
				if(tmin < temp)
				{
					hitType = new String(element.getType());
				}
				elements.offerLast(elements.pollFirst());
			}
			
			System.out.printf("%f \n", tmin);

			// Increment normally if greater than 1.0
			if((tmin / factor) > 1.0)
			{
				lp.x += ddx;
				lp.y += ddy;
				hitPoints1.add(new Point2D.Double(lp.x, lp.y));
				
				break;
			}
			else
			{
				System.out.println(hitType);
				switch(hitType)
				{
					case "Mirror":
						// If it is under 1.0, there will be at least one reflection.
						// Translate the point to the reflection point along the side.
						lp.x += ddx * (tmin / factor);
						lp.y += ddy * (tmin / factor);
						hitPoints1.offerLast(new Point2D.Double(lp.x, lp.y));

						// Calculate the CW (outward-pointing) perp vector for the side.
						double		vdx = pp2.x - pp1.x;	// Calc side vector
						double		vdy = pp2.y - pp1.y;	// from p1 to p2
						double		ndx = vdy;				// Calc perp vector:
						double		ndy = -vdx;				// negate x and swap

						// Need a NORMALIZED perp vector for the reflection calculation.
						double		nn = Math.sqrt(ndx * ndx + ndy * ndy);

						ndx = ndx / nn;	// Divide each coordinate by the length to
						ndy = ndy / nn;	// make a UNIT vector normal to the side.

						// Calculate v_reflected.
						double		dot = dot(vector1.x * 2, vector1.y * 2, 0.0, ndx, ndy, 0.0);
						double		vreflectedx = ddx - 2.0 * dot * ndx;
						double		vreflectedy = ddy - 2.0 * dot * ndy;

						// Reflect the update vector, and reduce it to compensate for
						// the distance the point moved to reach the side.
						ddx = vreflectedx * (1.0 - tmin / factor);
						ddy = vreflectedy * (1.0 - tmin / factor);

						// Also reflect the reference vector. It will change direction
						// but remain the same length.
						double		dot2 = dot(vector1.x, vector1.y, 0.0, ndx, ndy, 0.0);

						vector1.x -= 2.0 * dot2 * ndx;
						vector1.y -= 2.0 * dot2 * ndy;
						break;
					case "Lightbox":
						// Move lightpoint to the edge of the lightbox and stop it
						lp.x += ddx * (tmin / factor);
						lp.y += ddy * (tmin / factor);
						vector1.x = 0.0;
						vector1.y = 0.0;
						hitPoints1.offerLast(new Point2D.Double(lp.x, lp.y));
						break pointCalc;
				}
			}
		}
	}
	
	//**********************************************************************
	// Private Methods (Utility Functions)
	//**********************************************************************
	
	// Gets the tmin of object within the path of the lightpoint
	private double getTMin(Point2D.Double p1, Point2D.Double p2, double ddx, double ddy, 
			Point2D.Double pp1, Point2D.Double pp2, double tmin, Point2D.Double lp)
	{
		// Calculate the CW (outward-pointing) perp vector for the pair.
		double			vdx = p2.x - p1.x;		// Calc side vector
		double			vdy = p2.y - p1.y;		// from p1 to p2
		double			ndx = vdy;			// Calc perp vector:
		double			ndy = -vdx;				// negate x and swap
		
		// Check if point is inbetween
		double vdn = Math.sqrt(vdx * vdx + vdy * vdy);
		double nvdx = vdx / vdn;
		double nvdy = vdy / vdn;
		double pdx = lp.x + ddx - p1.x;
		double pdy = lp.y + ddy - p1.y;
		double dd = dot(nvdx, nvdy, 0.0, pdx, pdy, 0.0);
		if(!(dd >= 0 && dd <= vdn))
		{
			return tmin;
		}

		// See where the point intersects
		double			wdx = p1.x - lp.x;		// Calc test vector
		double			wdy = p1.y - lp.y;		// from q to p1

		// Calculate the top part of the t_hit equation.
		double			dnw = dot(ndx, ndy, 0.0, wdx, wdy, 0.0);

		// Check if the point is on the outside of the polygon. The dot
		// product will be 0 if the point is on a side, or slightly positive if
		// it is beyond it (which can happen due to roundoff error).
		if (dnw < 0.0)
		{
			// Calculate the bottom part of the t_hit equation.
			double	dnv = dot(ndx, ndy, 0.0, vector1.x * 2, vector1.y * 2, 0.0);

			// If the dot product is zero, the direction of motion is
			// parallel to the side. Disqualify it as a hit candidate
			// (even if the point is exactly ON the side).
			double	thit = ((dnv != 0.0) ? (dnw / dnv) : 0.0);

			// Remember the side with the smallest positive t_hit.
			// It's the side that the point will reach first.
			if ((0.0 < thit) && (thit < tmin))
			{
				pp1.setLocation(p1.x, p1.y);
				pp2.setLocation(p2.x, p2.y);
				return thit;
			}
		}
		return tmin;
	}
	
	private Point2D.Double getTransformedPoint(Point2D.Double p, Model.LightElement le)
	{
		double nx = p.x - le.getCenter().x;
		double ny = p.y - le.getCenter().y;
		double rotation = le.getRotation();
		double x = Math.cos(Math.toRadians(rotation))*nx - 
				Math.sin(Math.toRadians(rotation))*ny + le.getCenter().x;
		double y = Math.sin(Math.toRadians(rotation))*nx + 
				Math.cos(Math.toRadians(rotation))*ny + le.getCenter().y;

		return new Point2D.Double(x, y);
	}
	
	// Dot product method
	private double dot(double vx, double vy, double vz, double wx, double wy, double wz)
	{
		return (vx * wx + vy * wy + vz * wz);
	}
	
	// Sets color, normalizing r, g, b, a values from max 255 to 1.0.
	private void	setColor(GL2 gl, int r, int g, int b, int a)
	{
		gl.glColor4f(r / 255.0f, g / 255.0f, b / 255.0f, a / 255.0f);
	}

	// Sets fully opaque color, normalizing r, g, b values from max 255 to 1.0.
	private void	setColor(GL2 gl, int r, int g, int b)
	{
		setColor(gl, r, g, b, 255);
	}
}

//******************************************************************************
