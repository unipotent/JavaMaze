import java.util.Scanner; // import scanner for input
public class Maze
{
    public static void main(String[] args)
    {
        // these are the constant variables using final keyword because they should not change
        final float FLT_ROTATION_RATE = 10f; // the rate to rotate the player at, its used multiple times so use a const variable to hold it instead of repeating a magic number a bunch of times         
        final float FLT_RENDER_DISTANCE = 5f; // a distance reference to use for shading later also because its used multiple times
        
        // 2d array variables for screen and maze
        // remember that 2darray[0].length = number of items in that row
        // 2darray.length = number of rows
        // you can set it using 2darray[y][x]
        
        // also for some methods with the arrays it will cast the lengths (ints) to bytes or use bytes for indexing, remember this is allowed because all the bounds are both < 128
        
        // create a 2d char array to hold the screen using some bounds
        final char[][] ARR_SCREEN = new char[30][120];

        // this is the maze map where 1 means theres a wall and 0 means theres nothing
        // originally it was going to be a boolean[][] since there are only 2 states (wall, empty) but it is too hard to read 
        // and trying to use this and turn it into a boolean[][] with a method instead probably generates too much garbage for not much performance, they are also roughly the same size in memory
        final byte[][] ARR_MAZE = new byte[][]
        {
            {1,1,1,1,1,1,1,1},
            {1,0,1,0,0,0,0,1},
            {1,0,1,0,1,1,0,1},
            {1,0,1,0,0,1,0,1},
            {1,0,1,1,0,1,0,1},
            {1,0,0,0,0,1,0,1},
            {1,1,1,1,1,1,0,1}
        };
        // these variables are not final because they change
        float fltPlayerX = 1.5f, fltPlayerY = 1.5f; // this holds the player x and player y position, starting at roughly the top left corner

        float fltRotation = 90f; // holds player rotation in degrees, starting at 90 so they face the hallway
        char chrInput; // hold user input
        
        float fltNewPlayerX, fltNewPlayerY; // this is to hold values to check before setting new player positions, in case they are not valid
        float fltDirX, fltDirY; // this will hold the player direction as a vector
        float fltPerpX, fltPerpY; // this will hold a perpindicular vector to the direction vector for raycasting
        
        float fltCameraX; // this will hold the camera position in the for loop later
        float fltRayX, fltRayY; // this will hold the actual ray from the raycast
        
        float fltMagnitude; // this will be the magnitude of the ray to make a unit vector
        float fltUnitX, fltUnitY; // the unit vector
        
        byte bytIndexX, bytIndexY; // index variables to hold the xy coordinates of the maze while traversing it later
        float fltDistance; // this will hold the distance from the player to the nearest wall/exit along a ray vector
        
        // the wall height and upper and lower bounds of the wall to be drawn later
        float fltWallHeight;
        byte bytWallUpper, bytWallLower;
        
        // welcome screen 
        System.out.println("You are trapped in a maze and have to escape\n"+ 
        "Use W and S to move, use A and D to change directions, enter X to exit\n"+
        "Are you ready? Enter any key to continue");
        new Scanner(System.in).next(); // wait for them to enter anything, not populating the result into anything because it doesnt matter

        // this is a loop that runs as long as the player doesn't press the exit key or wins the game. use a do-while so that chrInput is populated before checking chrInput == .... and so the screen is rendered before any input
        do
        {
            // a unit vector given an angle is cos(theta), sin(theta)
            // the rotation is in degrees so turn it to radians because these functions expect radians
            // this holds the direction they are facing as a vector
            fltDirX = (float)Math.cos(Math.toRadians(fltRotation));
            fltDirY = (float)Math.sin(Math.toRadians(fltRotation));
            
            // a 2d vector perpindicular to another is (x,y) -> (-y, x)
            // this creates a 'screen' in front of the vector of the player direction for the raycasting later
            fltPerpX = -fltDirY;
            fltPerpY = fltDirX; 

            // this loop renders each column of the screen 
            for (byte bytScreenX = 0; bytScreenX < ARR_SCREEN[0].length; bytScreenX++)
            {
                // this gets each camera x positions using values from -1 to 1 so that 0 is exactly in the center 
                // this is a float instead of a byte because if it was just a byte then you would only have rays for the multiples -1,0,1 which is not correct
                // also multiply by 2.0f (a float) to trick the compiler into properly making this a float
                fltCameraX = 2.0f * bytScreenX / ARR_SCREEN[0].length - 1;

                // this creates a raycast from the player direction to the calculated position on the camera for each row
                // eg if you have bytCameraX = 0, meaning that should be the center of the screen, this vector will just be the direction vector + 0 which is correct.
                // or if you have bytCamera = 0.5 it will be the direction vector + half the screen plane which is correct

                // it will point to each place on the plane in this loop to render each row of the player fov.
                // calculate the x and y component of the vector
                fltRayX = fltDirX + fltCameraX * fltPerpX;
                fltRayY = fltDirY + fltCameraX * fltPerpY;
                
                // now that u have the ray vector, you can traverse the maze using the ray until you hit a wall/exit so you can render that wall or exit.
                // you can do this by incrementing the distance by a little bit, moving the index to be player position + distance * the unit ray vector, 
                //and checking each loop if the x and y as indexes on the maze array for those conditions
                
                // the rays as a unit vector
                fltMagnitude = getMagnitude(fltRayX, fltRayY);
                fltUnitX = fltRayX / fltMagnitude;
                fltUnitY = fltRayY / fltMagnitude;
                
                // reset distance to 0
                fltDistance = 0f;
                
                // implement the loop mentioned above
                // use a do while instead of a while, because the player should never start inside a wall so a starting notwallorexit(...) call is redundant 
                // and also so that the index variables are actually set before being checked
                do
                {
                    fltDistance += 0.01f;
                    
                    bytIndexX = (byte)(fltPlayerX + fltUnitX * fltDistance);
                    bytIndexY = (byte)(fltPlayerY + fltUnitY * fltDistance);
                } while (!atBounds(bytIndexX, bytIndexY, ARR_MAZE) && !isWall(ARR_MAZE[bytIndexY][bytIndexX]));
                // while not at bounds and not hit a wall, first checking if in bounds so that you dont try to check arrmaze at indexes that dont exist

                // walls look smaller the further the distance away from you, so you can use this distance to find a wall height to render
                // since its the denominator it will make the resulting number smaller as the distance grows bigger
                fltWallHeight = ARR_SCREEN.length / fltDistance;

                // the screen bound y divided by two should be the center, and then add half the wall height to get the upper bound of the wall
                // these are byte because the height of the screen is not big enough for a short, and these will be within the screen height
                bytWallUpper = (byte)(ARR_SCREEN.length / 2.0f + fltWallHeight / 2.0f);
                // the lower half will be the same thing except it will subtract the half wall height from the center instead
                bytWallLower = (byte)(ARR_SCREEN.length / 2.0f - fltWallHeight / 2.0f);

                // traverse that column of the screen and render each character using a for loop because you need to know the indexes to set unlike foreach or other loops
                for (byte bytScreenY = 0; bytScreenY < ARR_SCREEN.length; bytScreenY++)
                {
                    // if is in range of the wall bounds, that means this character should be a wall
                    if (bytScreenY <= bytWallUpper && bytScreenY >= bytWallLower)
                    {
                        // if that area is not a wall, but you still broke out of the loop and ended up here, that means its an exit because
                        // the only other break condition for the while loop was if you reached the end of the bounds
                        if (!isWall(ARR_MAZE[bytIndexY][bytIndexX]))
                        {
                            ARR_SCREEN[bytScreenY][bytScreenX] = ' '; // so render a special character for the exit
                        }
                        else // it is a wall
                        {
                            // make the wall a different shade depending on the distance, like to make it look like its lighter/fading the further it is
                            if (fltDistance < FLT_RENDER_DISTANCE * 0.25f) // less than quarter through distance
                            {
                                ARR_SCREEN[bytScreenY][bytScreenX] = '█'; 
                            }
                            else if (fltDistance < FLT_RENDER_DISTANCE * 0.5f) // less than halfway through the distance
                            {
                                ARR_SCREEN[bytScreenY][bytScreenX] = '▓';
                            }
                            else if (fltDistance < FLT_RENDER_DISTANCE * 0.75f) // 3 quarters through the distance
                            {
                                ARR_SCREEN[bytScreenY][bytScreenX] = '▒';
                            }
                            else if (fltDistance < FLT_RENDER_DISTANCE) // still in distance
                            {
                                ARR_SCREEN[bytScreenY][bytScreenX] = '░';
                            }
                            else // >= render distance, just make it blank because its too far
                            {
                                ARR_SCREEN[bytScreenY][bytScreenX] = '.';
                            }
                        }
                    }
                    else
                    {
                        ARR_SCREEN[bytScreenY][bytScreenX] = '.'; // not a wall, it should be the floor/ceiling
                    }
                }
            }
            // the screen should be fully updated after that loop so now render it onto the actual screen
            renderScreen(ARR_SCREEN);
            // print some information under the screen
            System.out.println("X: " + fltPlayerX + "Y: " + fltPlayerY);
            System.out.println("Your rotation: " + fltRotation);
            
            // process user input
            // get char input using .next() because you dont need the whole line and use touppercase to not be case sensitive
            // doesnt need try/catch because its just a character and no exception can be thrown from parsing
            chrInput = new Scanner(System.in).next().toUpperCase().charAt(0);
            switch(chrInput)
            {
                // if w or s, move in the direction of the direction vector by adding/subtracting their components from the player position point
                // also make sure you wont hit a wall by calling the method for error trapping, casting to byte to be a valid index and to match the method signature
                // then if you wont hit a wall, then update the playerposition to the new ones
                
                case 'W': 
                    {
                        // store these to not calculate it twice
                        fltNewPlayerX = getMovement(fltPlayerX, fltDirX);
                        fltNewPlayerY = getMovement(fltPlayerY, fltDirY);
                        if (inRangeAndNotWall((byte)fltNewPlayerX, (byte)fltNewPlayerY, ARR_MAZE))
                        {
                            fltPlayerX = fltNewPlayerX;
                            fltPlayerY = fltNewPlayerY; 
                        }
                    }
                    break;
                case 'S': 
                    {
                        // store these to not calculate it twice
                        fltNewPlayerX = getMovement(fltPlayerX, -fltDirX);
                        fltNewPlayerY = getMovement(fltPlayerY, -fltDirY);
                        if (inRangeAndNotWall((byte)fltNewPlayerX, (byte)fltNewPlayerY, ARR_MAZE))
                        {
                            fltPlayerX = fltNewPlayerX;
                            fltPlayerY = fltNewPlayerY; 
                        }
                    }
                    break;
                // if a or d rotate the player rotation in that direction
                case 'A': fltRotation -= FLT_ROTATION_RATE; break;
                case 'D': fltRotation += FLT_ROTATION_RATE; // no need for break on last statement
            }
        } while(chrInput != 'X' && !atExit((byte)fltPlayerX, (byte)fltPlayerY, ARR_MAZE)); // loop while they dont enter x to quit and they haven't won, casting to byte to be a valid index and to match the method signature
    }

    public static void renderScreen(char[][] ARR_SCREEN)
    {
        clearWindow(); // clear the terminal window
        // go down the y axis and render each line/char array
        // use foreach instead of for loop because you dont care about the index and youre just reading the items and not replacing the things in the array
        for (char[] arrRow : ARR_SCREEN)
        {
            // for each item in this row, print it out, using print and not println because its all on one line
            for (char chrChar : arrRow) 
                System.out.print(chrChar);
            System.out.println(""); // now use println to go down a row
        }
    }
    public static void clearWindow()
    {
        System.out.println('\u000c'); // clear the terminal window, this is a whole method because its hard to remember the character to print so reuse this method instead
    }

    public static float getMovement(float fltPlayerPos, float fltDirection) 
    {
        // get translation except divide the distance by some rate to make it less fast, without it it ends up being too fast and you look like youre teleporting
        return fltPlayerPos + fltDirection / 2;
    }
    public static float getMagnitude(float fltX, float fltY)
    {
        // It forms a right triangle with its components so you can find the hypotenuse of that triangle which is the magnitude/length of that vector
        // so use pythagorean theorem c^2 = a^2 + b^2 or c = sqrt(a^2 + b^2)
        return (float)Math.sqrt(fltX * fltX + fltY * fltY); // sqrt returns a double but thats too big so cast to float
    }
    public static boolean inRangeAndNotWall(byte bytX, byte bytY, byte[][] ARR_MAZE) // it checks that the x and y are in range and the maze at the xy is not a wall.
    {
        // cast the int lengths to bytes to match the method signature
        return inRange(bytX, (byte)ARR_MAZE[0].length) && inRange(bytY, (byte)ARR_MAZE.length) && !isWall(ARR_MAZE[bytY][bytX]);
    }

    public static boolean isWall(byte bytNode)
    {
        return bytNode == 1;  // it is a wall if it is a 1 
    }

    public static boolean inRange(byte bytValue, byte bytBound)
    {
        // this is a simple method so you dont have to write all of this whenever you check if an index is in range of an array given the length as bytBound
        return bytValue >= 0 && bytValue < bytBound; 
    }
    public static boolean atBounds(byte bytPlayerX, byte bytPlayerY, byte[][] ARR_MAZE)
    {
        // if exactly at start or end of each dimension of the array, then you are at the bounds
        return (bytPlayerX == ARR_MAZE[0].length -1 || bytPlayerX == 0) || (bytPlayerY == ARR_MAZE.length - 1|| bytPlayerY == 0);
    }

    public static boolean atExit(byte bytPlayerX, byte bytPlayerY, byte[][] ARR_MAZE)
    {
        // to be at an exit the conditions are: you have to be at the edge of the map, and you did not hit a wall
        
        // the conditions for being at an exit are: you are at the bounds, and there is not a wall there
        // since the player position will never be on a wall because of all the checks, so if you somehow ended up at the boundaries without hitting a wall, that has to be an exit if you are att the bounds
        // and you dont need to do an extra !isWall check
        boolean bolAtExit = atBounds(bytPlayerX, bytPlayerY, ARR_MAZE);
        
        // if they are then show a victory screen
        if (bolAtExit)
        {
            // clear terminal
            clearWindow();
            System.out.println("You found the exit, good job!"); // say a message
        }
        return bolAtExit; // return the result so that it will properly break because you have won and it should just show this screen
    }
}
