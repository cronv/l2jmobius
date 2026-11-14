/*
 * Copyright (c) 2013 L2jMobius
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR
 * IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package org.l2jmobius.tools.map;

/**
 * All geodata builder tuning constants, calibrated by in-game testing around Gludio and Giran.
 * @author Mobius
 */
public class GeoConfig
{
	public static final double GEODATA_Z_OFFSET = 28.3;
	public static final int CELLS = 2048; // cells per region axis (16 game units each)
	
	public static final int MAX_LAYERS = 16; // Giran clan hall stacks 5 real floors and starved 4 slots; the Orc King Hall mountain shell stacks EIGHT surfaces above its terrain-floor entrance corridor and starved 8. The reachability pass drops dead shell layers, so the written file barely grows.
	public static final double LAYER_MERGE = 48; // surfaces closer than this merge into one layer
	public static final double MAX_CLIMB = 16; // on REAL (unquantized) heights: standard 16-unit stair risers stay open, anything steeper closes. Lowering this (11, 10, 8 all tried in-game) closes the wedge-seams but breaks real staircases with it - seam lips are closed by the BEVEL rule below instead.
	public static final double STAIR_CLIMB = 32; // climb cap INSIDE stair-class mesh footprints (both cells): a mesh that calls itself a stair is built to be climbed, and its risers run past MAX_CLIMB (Giran church square flight: 19.9 real between terrace and first tread). Stays under WALL_COLUMN, so cheek walls and parapets still block.
	public static final double BEVEL_NZ = 0.94; // walkable surfaces sloped steeper than ~20 degrees are bevel-class: a stair-sized-plus rise crossing a boundary OVER such a joint closes (butt-joined pavement sheets meet in short 45-degree bevels that wedge the client capsule). Flat stair treads (nz~1) and vertical risers (wall-class) never qualify.
	public static final int FLAT_RADIUS = 1; // flat-neighborhood walkability: every cell within this many cells must sit within 8 units for the ground to count as genuinely flat (2 = the 5x5 / 24-neighbour test)
	public static final double STAND_LIP = 48; // non-walkable lips (curbs, step faces) up to this height above a walkable surface stay standable
	public static int[] GAPS_BOX = null; // {cx1, cy1, cx2, cy2} region-local box for targeted continuity closing
	public static final double STEEP_NZ = 0.707; // steeper than 45 degrees = wall-class surface (targets true wall-faces like the Giran harbor buttress). The steep-surface NSWE closure rule it once gated was removed: tested at 20 and 45 degrees (two-way) and re-tested 2026-07-14 after the real-height/quarter-point/wall-column sampler changes, it still over-closed (~89k extra closures vs reference on 19_21) and per-layer steep flags cannot see boundary-straddling bevel slivers anyway
	public static final double STAIRS_HEIGHT = 10; // climbs this small ignore the steep rule (stair-sized steps)
	public static final double WATER_FALL_MIN = 100; // falls higher than this landing BELOW the water surface close the edge (quay walls); smaller drops keep shores swimmable (AllowWater=true)
	public static final double ABYSS_FALL = 1200; // falls deeper than this close the edge: lethal-depth chasms (dark elf village walkways 3100 over the pit - the web railings carry no collision geometry at all). Ordinary cliff/ledge drops stay far below this; small-drop closures were tested at 64-512 and over-close.
	public static final double CRAWL_GAP = 88; // layers with less than this to their in-cell ceiling are crawl space; only INTERIOR cells of covered regions purge (see the erosion pass) so 1-cell eave rims, doorways and porches survive. Calibrated to client clearance: the Giran bank crossbar at 85.3 is client-walkable, so anything at 88+ is real walking headroom - the Gludio shop porches float 104-112 over the street and the client walks chars under them at street level (no collidable steps exist), matching the reference. Wall opacity is carried by the wall-column verdict, not by purging covered floors.
	public static final double WALL_SHADOW = 240; // covered ground hugging a TALL wall face purges up to this ceiling gap (the wall-adjacent line-of-sight rule): building overhang strips (Gludio magic shop balcony floats 224 over the street) go roof-only like the reference so sight lines die AT the wall base, while free-standing covered walkways and high halls (naves 300+) keep their ground. Door lanes survive - their boundaries carry no wall intervals.
	public static final double WALL_COLUMN = 40; // wall-column verdict: a wall chain rising from walking level to more than this above the floor turns the cell into a solid wall-top column - line-of-sight blocks by HEIGHT, so walls must carry their top height, not directional bits on ground cells. Calibrated: above 40 = wall (fences and parapets included), 40 and below = climbable step/curb profile.
	public static final double CHAR_HEIGHT_GEOM = 80; // standing headroom for ALL floors, measured by a CENTER ray. Client-passable tight spots calibrate the bound: Giran bank gate crossbar 85.3 over the floor, temple arch rib 93 - both walkable, so <= 85 must pass. Off-center decor (balusters, vault sides) must not kill floors - only the obstruction above the cell CENTER counts. Phantom crawlspaces (buried terrain 192 under town pavement) pass this test but die in the reachability flood fill.
	public static final double WALL_WINDOW_TOP = 76; // head-window top for wall-edge closures: vault springs/arch sides 85-92 and shopfront fascia beams at 79 (Giran armor shop) cross boundaries the client walks under - they must not close edges (diagonal moves die on N/S bits via the engine's anti-corner-cut), while sills and lintels at 50-76 must.
	public static final int SEALED_ROOM_CELLS = 256; // sealed-interior rescue: an unreached layer component covering more than this many cells is a real room and keeps its floors. Instance halls (the Throne of Heroes boss room sits 8700 below terrain) have no walkable way in from the surface, so the reachability flood never seeds them and their floors dropped, leaving only the cavern shell 1156 overhead for line of sight to hit. 256 cells is a 16x16 room; the buried slivers the flood exists to drop span a handful.
	public static final int[] DEBUG_CELL; // -DdebugCell=cx,cy dumps that cell's raw spans during buildGrid
	
	static
	{
		final String debugCell = System.getProperty("debugCell");
		if (debugCell == null)
		{
			DEBUG_CELL = null;
		}
		else
		{
			final String[] parts = debugCell.split(",");
			DEBUG_CELL = new int[]
			{
				Integer.parseInt(parts[0].trim()),
				Integer.parseInt(parts[1].trim())
			};
		}
	}
	
	public static final double WALL_NZ = 0.5; // |normal.z| below this = a wall (slope steeper than 60 degrees; floors keep their sampled heights - steepness is handled by the wall-column and bevel rules instead)
}
