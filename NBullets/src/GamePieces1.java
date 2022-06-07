import tester.*; // The tester library
import javalib.worldimages.*; // images, like RectangleImage or OverlayImages
import javalib.funworld.*; // the abstract World class and the big-bang library
import java.awt.Color;

//to represent a position
class MyPosn extends Posn {
  // standard constructor
  MyPosn(int x, int y) {
    super(x, y);
  }

  // constructor to convert from a Posn to a MyPosn
  MyPosn(Posn p) {
    this(p.x, p.y);
  }

  // given a MyPosn adds to this MyPosn
  MyPosn add(MyPosn p) {
    return new MyPosn(this.x + p.x, this.y + p.y);
  }

  // determines if this MyPosn is off the screen
  boolean isOffScreen(int width, int height) {
    return this.x < 0 || this.x > width || this.y < 0 || this.y > height;
  }
}

//to represent a game piece
interface IGamePiece {
  AGamePiece move();
}

//to represent an abstract game piece
abstract class AGamePiece implements IGamePiece {
  MyPosn position; // in pixels
  MyPosn velocity; // in pixels/tick
  int radius;
  Color color;

  // the constructor
  AGamePiece(MyPosn position, MyPosn velocity, int radius, Color color) {
    this.position = position;
    this.velocity = velocity;
    this.radius = radius;
    this.color = color;
  }

  // determines if this game piece is off screen given a width and a height
  boolean isOffScreen(int width, int height) {
    return this.position.isOffScreen(width + this.radius, height + this.radius);
  }

  // draws this game piece
  WorldImage draw() {
    return new CircleImage(this.radius, OutlineMode.SOLID, this.color);
  }

  // places this game piece in its appropriate position on the screen
  WorldScene place(WorldScene scene) {
    return scene.placeImageXY(this.draw(), this.position.x, this.position.y);
  }

  // determines if this game piece has collided with that game piece
  public boolean collidesWith(AGamePiece other) {
    return ((this.position.x - other.position.x) * (this.position.x - other.position.x)
        + (this.position.y - other.position.y)
            * (this.position.y - other.position.y)) <= (this.radius + other.radius)
                * (this.radius + other.radius);
  }

  // explosion of a general game piece
  public ILoGamePiece explode() {
    return new MtLoGamePiece();
  }
}

//to represent a ship game piece
class Ship extends AGamePiece {
  boolean left;

  // constructor
  Ship(MyPosn position, boolean left) {
    super(position, new MyPosn(4, 0), 10, Color.cyan);
    this.left = left;
  }

  // moves this ship to its new position after one tick
  public AGamePiece move() {
    if (this.left) {
      return new Ship(this.position.add(this.velocity), this.left);
    }
    else {
      return new Ship(this.position.add(new MyPosn(-this.velocity.x, -this.velocity.y)), 
          this.left);
    }
  }
}

//to represent a bullet
class Bullet extends AGamePiece {
  int n; // the number of explosion

  // constructor
  Bullet(MyPosn position, MyPosn velocity, int radius, int n) {
    super(position, velocity, Math.min(radius, 10), Color.pink);
    this.n = n;
  }

  // moves this game piece to its new position after one tick
  public AGamePiece move() {
    return new Bullet(this.position.add(this.velocity), this.velocity, this.radius, this.n);
  }

  @Override
  // explodes this bullet into the correct amount of bullets
  public ILoGamePiece explode() {
    return explodeHelp(this.n + 1);
  }

  // helps create the correct amount of new bullets for an explosion
  private ILoGamePiece explodeHelp(int i) {
    if (i == 0) {
      return new MtLoGamePiece();
    }
    else {
      MyPosn velocity = new MyPosn((int) (8 * (Math.cos(Math.toRadians(i * 360 / (this.n + 1))))),
          (int) (8 * (Math.sin(Math.toRadians(i * 360 / (this.n + 1))))));
      return new ConsLoGamePiece(new Bullet(this.position, velocity, this.radius + 2, this.n + 1),
          this.explodeHelp(i - 1));
    }
  }
}

// to represent a list of game pieces
interface ILoGamePiece {
  // moves all the game pieces in this list of game pieces
  ILoGamePiece moveAll();

  // removes all the game pieces in this list of game pieces that are off screen
  ILoGamePiece removeOffScreen(int width, int height);

  // places all the game pieces in this list of game pieces
  WorldScene placeAll(WorldScene s);

  // determines if the game piece collides with any in this list
  boolean collidesWithAny(AGamePiece gamePiece);

  // determines if any in this list collide with any in the other list
  boolean anyCollidesWithAny(ILoGamePiece other);

  // removes a game piece from this list if it collides with any in the other list
  ILoGamePiece removeCollisions(ILoGamePiece other);

  // adds the other list of game pieces to this one
  ILoGamePiece append(ILoGamePiece other);

  // returns a list of all the new bullets created from bullet explosions
  ILoGamePiece allNewBullets(ILoGamePiece ships);

  // calculates the amount of collisions
  int countCollisions(ILoGamePiece other);

  // determines if empty
  boolean isEmpty();
}

//to represent an empty list of game pieces
class MtLoGamePiece implements ILoGamePiece {
  // moves all the circles in this empty list of game pieces
  public ILoGamePiece moveAll() {
    return this;
  }

  // removes all the game pieces in this empty list of game pieces that are off
  // screen
  public ILoGamePiece removeOffScreen(int width, int height) {
    return this;
  }

  // places all the game pieces in this empty list of game pieces
  public WorldScene placeAll(WorldScene scene) {
    return scene;
  }

  // determines if the game piece collides with any game pieces in this list
  public boolean collidesWithAny(AGamePiece gamePiece) {
    return false;
  }

  // determines if any in this list collides with any in the other list
  public boolean anyCollidesWithAny(ILoGamePiece other) {
    return false;
  }

  // removes collisions in this empty list of game pieces
  public ILoGamePiece removeCollisions(ILoGamePiece other) {
    return this;
  }

  // calculates the amount of collisions
  public int countCollisions(ILoGamePiece other) {
    return 0;
  }

  // appends the other list of game pieces to this empty one
  public ILoGamePiece append(ILoGamePiece other) {
    return other;
  }

  // returns a list of all the new bullets created from bullet explosions
  public ILoGamePiece allNewBullets(ILoGamePiece ships) {
    return this;
  }

  // determines if empty
  public boolean isEmpty() {
    return true;
  }
}

//to represent a nonempty list of game pieces 
class ConsLoGamePiece implements ILoGamePiece {
  AGamePiece first;
  ILoGamePiece rest;

  // the constructor
  ConsLoGamePiece(AGamePiece first, ILoGamePiece rest) {
    this.first = first;
    this.rest = rest;
  }

  // moves all the game pieces in this nonempty list of game pieces
  public ILoGamePiece moveAll() {
    return new ConsLoGamePiece(this.first.move(), this.rest.moveAll());
  }

  // removes all the game pieces in this nonempty list of game pieces that are off
  // screen
  public ILoGamePiece removeOffScreen(int width, int height) {
    if (this.first.isOffScreen(width, height)) {
      return this.rest.removeOffScreen(width, height);
    }
    else {
      return new ConsLoGamePiece(this.first, this.rest.removeOffScreen(width, height));
    }
  }

  // places all the game pieces in this nonempty list of game pieces
  public WorldScene placeAll(WorldScene scene) {
    return this.rest.placeAll(this.first.place(scene));
  }

  // collides with any
  public boolean collidesWithAny(AGamePiece gamePiece) {
    return gamePiece.collidesWith(this.first) || this.rest.collidesWithAny(gamePiece);
  }

  public boolean anyCollidesWithAny(ILoGamePiece other) {
    return other.collidesWithAny(this.first) || this.rest.anyCollidesWithAny(other);
  }

  // removes collisions
  public ILoGamePiece removeCollisions(ILoGamePiece other) {
    if (other.collidesWithAny(this.first)) {
      return this.rest.removeCollisions(other);
    }
    else {
      return new ConsLoGamePiece(this.first, this.rest.removeCollisions(other));
    }
  }

  // calculates the amount of collisions
  public int countCollisions(ILoGamePiece other) {
    if (other.collidesWithAny(this.first)) {
      return 1 + this.rest.countCollisions(other);
    }
    else {
      return this.rest.countCollisions(other);
    }
  }

  // appends the other list of game pieces to this one
  public ILoGamePiece append(ILoGamePiece other) {
    return new ConsLoGamePiece(this.first, this.rest.append(other));
  }

  // returns a list of all the new bullets created from bullet explosions
  public ILoGamePiece allNewBullets(ILoGamePiece ships) {
    if (ships.collidesWithAny(this.first)) {
      return this.first.explode().append(this.rest.allNewBullets(ships));
    }
    else {
      return this.rest.allNewBullets(ships);
    }
  }

  // determines if empty
  public boolean isEmpty() {
    return false;
  }
}

//to represent examples and test of game pieces
class ExamplesGamePieces {
  ExamplesGamePieces() {
  }

  WorldScene scene = new WorldScene(30, 30);
  MyPosn posn0 = new MyPosn(1, -1);
  MyPosn posn1 = new MyPosn(5, 6);
  MyPosn posn2 = new MyPosn(6, 5);
  MyPosn posn3 = new MyPosn(11, 11);

  AGamePiece ship1 = new Ship(this.posn1, false);
  AGamePiece ship2 = new Ship(this.posn0, false);
  AGamePiece ship3 = new Ship(this.posn3, true);

  AGamePiece bullet1 = new Bullet(this.posn1, new MyPosn(0, -1), 4, 1);
  AGamePiece bullet2 = new Bullet(this.posn0, new MyPosn(0, -1), 2, 0);
  AGamePiece bullet3 = new Bullet(this.posn3, new MyPosn(0, -1), 6, 2);
  AGamePiece bullet4 = new Bullet(this.posn2, new MyPosn(0, -1), 2, 0);
  AGamePiece bulllet5 = new Bullet(this.posn3, new MyPosn(8, 0), 6, 2);
  AGamePiece bulllet6 = new Bullet(this.posn3, new MyPosn(-8, 0), 6, 2);

  WorldScene oneShip = scene.placeImageXY(new CircleImage(10, OutlineMode.SOLID, Color.cyan),
      5, 6);
  WorldScene twoShip = oneShip.placeImageXY(new CircleImage(10, OutlineMode.SOLID, Color.cyan), 0,
      0);
  WorldScene ThreeShip = oneShip.placeImageXY(new CircleImage(10, OutlineMode.SOLID, Color.cyan),
      11, 11);
  WorldScene sceneWithBullets = scene
      .placeImageXY(new TextImage("BULLETS LEFT: " + 10, Color.BLACK), 60, 25);
  WorldScene sceneWithInfo = sceneWithBullets
      .placeImageXY(new TextImage("SHIPS DESTROYED: " + 0, Color.BLACK), 500 - 75, 25);
  WorldScene sceneWithShoot = sceneWithInfo
      .placeImageXY(new HexagonImage(20, OutlineMode.SOLID, Color.GREEN), 500 / 2, 300 - 20);
  WorldScene endScene = new WorldScene(500, 300);
  WorldScene gameOver = endScene.placeImageXY(new TextImage("Game Over", Color.red), 250, 250);

  MyGame world = new MyGame(500, 300, 10);
  MyGame shipworld = new MyGame(500, 300, 1, 10, 0, this.onlyship, this.mt);
  MyGame bulletworld = new MyGame(500, 300, 1, 10, 0, this.mt, this.allbullets);
  MyGame fullworld = new MyGame(500, 300, 1, 10, 0, this.onlyship, this.allbullets);
  MyGame endworld = new MyGame(500, 300, 0);

  MtLoGamePiece mt = new MtLoGamePiece();
  ILoGamePiece onlyship = new ConsLoGamePiece(this.ship1, this.mt);
  ILoGamePiece allships = new ConsLoGamePiece(this.ship1,
      new ConsLoGamePiece(this.ship2, new ConsLoGamePiece(this.ship3, this.mt)));
  ILoGamePiece allbullets = new ConsLoGamePiece(this.bullet1,
      new ConsLoGamePiece(this.bullet2, new ConsLoGamePiece(this.bullet3, this.mt)));
  ILoGamePiece sandb = new ConsLoGamePiece(this.ship1, new ConsLoGamePiece(this.bullet4, this.mt));

  ILoGamePiece newBullets = this.allbullets.allNewBullets(this.allships);
  ILoGamePiece untouchedBullets = this.allbullets.removeCollisions(this.allships);
  ILoGamePiece untouchedShips = this.allships.removeCollisions(this.allbullets);

  /////////////////////////////////////////////////////////////////////////////////////////////////

  // tests the add function for MyPosn
  boolean testAdd(Tester t) {
    return t.checkExpect(this.posn1.add(posn2), this.posn3)
        && t.checkExpect(this.posn1.add(posn0), this.posn1)
        && t.checkExpect(this.posn3.add(posn2), new MyPosn(17, 16));
  }

  // tests the isOffScreen function in the AGamePiece class
  boolean testOffScreen(Tester t) {
    return t.checkExpect(this.posn1.isOffScreen(7, 7), false)
        && t.checkExpect(this.posn3.isOffScreen(7, 7), true)
        && t.checkExpect(this.posn0.isOffScreen(7, 7), true)
        && t.checkExpect(this.ship1.isOffScreen(7, 7), false)
        && t.checkExpect(this.ship2.isOffScreen(7, 7), true)
        && t.checkExpect(this.ship3.isOffScreen(7, 7), false)
        && t.checkExpect(this.bullet1.isOffScreen(7, 7), false)
        && t.checkExpect(this.bullet2.isOffScreen(7, 7), true)
        && t.checkExpect(this.bullet3.isOffScreen(7, 7), false);

  }

  // tests the draw function in the AGamePiece class
  boolean testDraw(Tester t) {
    return t.checkExpect(this.ship1.draw(), new CircleImage(10, OutlineMode.SOLID, Color.cyan))
        && t.checkExpect(this.bullet1.draw(), new CircleImage(4, OutlineMode.SOLID, Color.pink))
        && t.checkExpect(this.bullet2.draw(), new CircleImage(2, OutlineMode.SOLID, Color.pink))
        && t.checkExpect(this.bullet3.draw(), new CircleImage(6, OutlineMode.SOLID, Color.pink));
  }

  // tests the place function in the AGamePiece class
  boolean testPlace(Tester t) {
    return t.checkExpect(this.ship1.place(this.scene),
        this.scene.placeImageXY(this.ship1.draw(), 5, 6))
        && t.checkExpect(this.bullet3.place(this.scene),
            this.scene.placeImageXY(this.bullet3.draw(), 11, 11));
  }

  // tests the colidesWith function in the AGamePiece class
  boolean testColidesWith(Tester t) {
    return t.checkExpect(this.bullet1.collidesWith(this.ship1), true)
        && t.checkExpect(this.bullet2.collidesWith(this.ship3), false);
  }

  // tests the function explode in the AGamePiece class
  boolean testExplode(Tester t) {
    return t.checkExpect(this.bullet1.explode(),
        new ConsLoGamePiece(this.bulllet5, new ConsLoGamePiece(this.bulllet6, this.mt)))
        && t.checkExpect(this.ship1.explode(), new MtLoGamePiece());
  }

  // tests the move function in the AGamePiece class
  boolean testMove(Tester t) {
    return t.checkExpect(this.ship1.move(), new Ship(this.posn1.add(new MyPosn(-4, -0)), false))
        && t.checkExpect(this.ship3.move(), new Ship(this.posn3.add(new MyPosn(4, 0)), true))
        && t.checkExpect(this.bullet1.move(),
            new Bullet(this.posn1.add(new MyPosn(0, -1)), new MyPosn(0, -1), 4, 1));
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////

  // tests the moveAll function in the ILoGamePiece class
  boolean testMoveAll(Tester t) {
    return t.checkExpect(this.mt.moveAll(), this.mt)
        && t.checkExpect(this.allships.moveAll(),
            new ConsLoGamePiece(this.ship1.move(),
                new ConsLoGamePiece(this.ship2.move(),
                    new ConsLoGamePiece(this.ship3.move(), this.mt))))
        && t.checkExpect(this.allbullets.moveAll(),
            new ConsLoGamePiece(this.bullet1.move(),
                new ConsLoGamePiece(this.bullet2.move(),
                    new ConsLoGamePiece(this.bullet3.move(), this.mt))))
        && t.checkExpect(this.sandb.moveAll(), new ConsLoGamePiece(this.ship1.move(),
            new ConsLoGamePiece(this.bullet4.move(), this.mt)));
  }

  // tests the removeOffScreen function in the ILoGamePiece class
  boolean testRemoveOffScreen(Tester t) {
    return t.checkExpect(this.mt.removeOffScreen(7, 7), this.mt)
        && t.checkExpect(this.allships.removeOffScreen(7, 7),
            new ConsLoGamePiece(this.ship1, new ConsLoGamePiece(this.ship3, this.mt)))
        && t.checkExpect(this.allbullets.removeOffScreen(7, 7),
            new ConsLoGamePiece(this.bullet1, new ConsLoGamePiece(this.bullet3, this.mt)))
        && t.checkExpect(this.sandb.removeOffScreen(7, 7),
            new ConsLoGamePiece(this.ship1, new ConsLoGamePiece(this.bullet4, this.mt)))
        && t.checkExpect(this.world.removeOffScreen(), this.world);
  }

  // tests the placeAll function in the ILoGamePiece class
  boolean testPlaceAll(Tester t) {
    return t.checkExpect(this.mt.placeAll(this.scene), this.scene)
        && t.checkExpect(this.allships.placeAll(this.scene), this.ThreeShip);
  }

  // tests the collidesWithAny function in the ILoGamePiece class
  boolean testCollidesWithAny(Tester t) {
    return t.checkExpect(this.mt.collidesWithAny(bullet1), false)
        && t.checkExpect(this.allships.collidesWithAny(bullet1), true);
  }

  // tests the anyCollidesWithAny function in the ILoGamePiece class
  boolean testAnyCollidesWithAny(Tester t) {
    return t.checkExpect(this.mt.anyCollidesWithAny(this.mt), false)
        && t.checkExpect(this.mt.anyCollidesWithAny(this.allships), false)
        && t.checkExpect(this.allships.anyCollidesWithAny(this.allbullets), true);
  }

  // tests the removeCollisions function in the ILoGamePiece class
  boolean testRemoveCollisions(Tester t) {
    return t.checkExpect(this.mt.removeCollisions(this.mt), this.mt)
        && t.checkExpect(this.mt.removeCollisions(this.allbullets), this.mt)
        && t.checkExpect(this.allships.removeCollisions(this.allbullets), this.mt);
  }

  // tests the countCollisions function in the ILoGamePiece class
  boolean testCountCollisions(Tester t) {
    return t.checkExpect(this.mt.countCollisions(this.mt), 0)
        && t.checkExpect(this.allships.countCollisions(this.mt), 0)
        && t.checkExpect(this.allships.countCollisions(this.allbullets), 3);
  }

  // tests the append function in the ILoGamePiece class
  boolean testAppend(Tester t) {
    return t.checkExpect(this.mt.append(this.mt), this.mt)
        && t.checkExpect(this.allships.append(this.mt), this.allships)
        && t.checkExpect(this.allships.append(this.allbullets), new ConsLoGamePiece(this.ship1,
            new ConsLoGamePiece(this.ship2, new ConsLoGamePiece(this.ship3, new ConsLoGamePiece(
                this.bullet1,
                new ConsLoGamePiece(this.bullet2, new ConsLoGamePiece(this.bullet3, this.mt)))))));
  }

  // tests the allNewBullets function in the ILoGamePiece class
  boolean testAllNewBullets(Tester t) {
    return t.checkExpect(this.mt.allNewBullets(this.allships), this.mt)
        && t.checkExpect(this.allbullets.allNewBullets(this.allships),
            this.bullet1.explode().append(
                new ConsLoGamePiece(this.bullet2, new ConsLoGamePiece(this.bullet3, this.mt))
                    .allNewBullets(allships)));
  }

  // tests the isEmpty function in the ILoGamePiece class
  boolean testIsEmpty(Tester t) {
    return t.checkExpect(this.mt.isEmpty(), true) && t.checkExpect(this.allships.isEmpty(), false);
  }
  /////////////////////////////////////////////////////////////////////////////////////////////////

  // to test the bigBang function in the MyGame class
  boolean testBigBang(Tester t) {
    MyGame world = new MyGame(500, 300, 10);
    // width, height, tickrate = 0.5 means every 0.5 seconds the onTick method will
    // get called.
    return world.bigBang(500, 300, 1.0 / 28.0);
  }

  // to test the addInfo function in the MyGame class
  boolean testAddInfo(Tester t) {
    return t.checkExpect(this.world.addInfo(this.scene), this.sceneWithShoot);
  }

  // to test the onTick function in the MyGame class
  boolean testOnTick(Tester t) {
    return t.checkExpect(this.world.onTick(), new MyGame(500, 300, 2, 10, 0, this.mt, this.mt));
  }

  // to test the movePieces function in the MyGame class
  boolean testMovePieces(Tester t) {
    return t.checkExpect(this.world.movePieces(), new MyGame(500, 300, 2, 10, 0, this.mt, this.mt));
  }

  // to test the addShips method in the MyGame class
  boolean testAddShips(Tester t) {
    return t.checkExpect(this.world.addShips(), this.world)
        && t.checkExpect(this.endworld.addShips(), this.endworld)
        && t.checkExpect(this.shipworld.addShips(), new MyGame(500, 300, 1, 10, 0, null, null));
  }

  // to test the addShip method in the MyGame class
  boolean testAddShip(Tester t) {
    return t.checkExpect(this.world.addShip(), this.world)
        && t.checkExpect(this.endworld.addShip(), this.endworld)
        && t.checkExpect(this.shipworld.addShip(), new MyGame(500, 300, 1, 10, 0, null, null));
  }

  // to test the shipsDestroyed function in the MyGame class
  boolean testshipsDestroyed(Tester t) {
    return t.checkExpect(this.world.shipsDestroyed(), this.world);
  }

  // to test the removeShips function in the MyGame class
  boolean testRemoveShips(Tester t) {
    return t.checkExpect(this.world.removeShips(), this.world);
  }

  // to test the removeBullets function in the MyGame class
  boolean testRemoveBullets(Tester t) {
    return t.checkExpect(this.world.removeBullets(), this.world);
  }

  // to test the addBullets function in the MyGame class
  boolean testAddBullets(Tester t) {
    return t.checkExpect(this.world.addBullets(), this.world);

  }

  // to test the onKeyEvent function in the MyGame class
  boolean testOnKeyEvent(Tester t) {
    return t.checkExpect(this.world.onKeyEvent("x"), this.world) && t.checkExpect(
        this.world.onKeyEvent(" "), new MyGame(500, 300, 1, 9, 0, this.mt, new ConsLoGamePiece(
            new Bullet(new MyPosn(250, 260), new MyPosn(0, -8), 2, 1), this.mt)));
  }

  // to test the WorldEnds function in the MyGame class
  boolean testWorldEnds(Tester t) {
    return t.checkExpect(this.world.worldEnds(), new WorldEnd(false, world.makeEndScene()))
        && t.checkExpect(this.endworld.worldEnds(), new WorldEnd(true, endworld.makeEndScene()));
  }

  // to test the makeEndScene function in the MyGame class
  boolean testMakeEndScene(Tester t) {
    return t.checkExpect(this.world.makeEndScene(),
        gameOver.placeImageXY(new TextImage("Ships Destoyed: " + 0, Color.black), 250, 275));
  }
}