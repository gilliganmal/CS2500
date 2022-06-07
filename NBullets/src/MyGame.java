import javalib.worldimages.*;
import javalib.funworld.*;
import java.awt.Color;
import java.util.Random;

///to represent a world state
class MyGame extends World {
  int width;
  int height;
  int currentTick;
  int bulletsLeft;
  int shipsDestroyed;
  ILoGamePiece ships;
  ILoGamePiece bullets;

  // the user constructor
  MyGame(int width, int height, int bulletsLeft) {
    this(width, height, 1, bulletsLeft, 0, new MtLoGamePiece(), new MtLoGamePiece());
  }

  // my constructor
  MyGame(int width, int height, int currentTick, int bulletsLeft, int shipsDestroyed,
      ILoGamePiece ships, ILoGamePiece bullets) {
    if (width < 0 || height < 0 || bulletsLeft < 0) {
      throw new IllegalArgumentException("Invalid arguments passed to constructor.");
    }
    else {
      this.width = width;
      this.height = height;
      this.currentTick = currentTick;
      this.bulletsLeft = bulletsLeft;
      this.shipsDestroyed = shipsDestroyed;
      this.ships = ships;
      this.bullets = bullets;
    }
  }

  @Override
  // to make a scene
  public WorldScene makeScene() {
    WorldScene scene = new WorldScene(this.width, this.height);
    scene = addInfo(scene);
    scene = ships.placeAll(scene);
    scene = bullets.placeAll(scene);
    return scene;
  }

  // displays this game information on the scene
  WorldScene addInfo(WorldScene scene) {
    WorldScene sceneWithBullets = scene
        .placeImageXY(new TextImage("BULLETS LEFT: " + this.bulletsLeft, Color.BLACK), 60, 25);
    WorldScene sceneWithInfo = sceneWithBullets.placeImageXY(
        new TextImage("SHIPS DESTROYED: " + this.shipsDestroyed, Color.BLACK), this.width - 75, 25);
    WorldScene sceneWithShoot = sceneWithInfo.placeImageXY(
        new HexagonImage(20, OutlineMode.SOLID, Color.GREEN), this.width / 2, this.height - 20);
    return sceneWithShoot;
  }

  @Override
  // This method gets called every tickrate seconds (see bellow in example class).
  public MyGame onTick() {
    return this.shipsDestroyed().addShips().addBullets().movePieces().removeOffScreen();
  }

  public MyGame removeOffScreen() {
    return new MyGame(this.width, this.height, this.currentTick, this.bulletsLeft,
        this.shipsDestroyed, this.ships.removeOffScreen(this.width, this.height),
        this.bullets.removeOffScreen(this.width, this.height));
  }

  // moves all of the pieces in this game
  public MyGame movePieces() {
    return new MyGame(this.width, this.height, this.currentTick + 1, this.bulletsLeft,
        this.shipsDestroyed, this.ships.moveAll(), this.bullets.moveAll());
  }

  // adds a random number in [1,3] of ships to this game
  public MyGame addShips() {
    int amt = new Random().nextInt(3) + 1;
    if (amt == 1) {
      return addShip();
    }
    else if (amt == 2) {
      return addShip().addShip();
    }
    else {
      return addShip().addShip().addShip();
    }
  }

  // adds a random ship to this game
  public MyGame addShip() {
    boolean left = new Random().nextBoolean();
    int x;
    if (left) {
      x = 0;
    }
    else {
      x = this.width;
    }
    int y = new Random().nextInt(this.height - (2 * this.height / 7) + 1) + (this.height / 7);
    Ship ship = new Ship(new MyPosn(x, y), left);
    if (this.currentTick % 28 == 0) {
      return new MyGame(this.width, this.height, this.currentTick, this.bulletsLeft,
          this.shipsDestroyed, new ConsLoGamePiece(ship, this.ships), this.bullets);
    }
    else {
      return this;
    }
  }

  // increments the count of ships destroyed
  public MyGame shipsDestroyed() {
    int shipsDestroyed = this.ships.countCollisions(this.bullets);
    return new MyGame(this.width, this.height, this.currentTick, this.bulletsLeft,
        this.shipsDestroyed + shipsDestroyed, this.ships, this.bullets);
  }

  // removes the ships that have collided with a bullet from this game
  public MyGame removeShips() {
    return new MyGame(this.width, this.height, this.currentTick, this.bulletsLeft,
        this.shipsDestroyed, this.ships.removeCollisions(this.bullets), this.bullets);
  }

  // removes the bullets that have collided with a ship from this game
  public MyGame removeBullets() {
    return new MyGame(this.width, this.height, this.currentTick, this.bulletsLeft,
        this.shipsDestroyed, this.ships, this.bullets.removeCollisions(this.ships));
  }

  // adds the new bullets to the game after a collision
  public MyGame addBullets() {
    ILoGamePiece newBullets = this.bullets.allNewBullets(this.ships);
    ILoGamePiece untouchedBullets = this.bullets.removeCollisions(this.ships);
    ILoGamePiece untouchedShips = this.ships.removeCollisions(this.bullets);
    return new MyGame(this.width, this.height, this.currentTick, this.bulletsLeft,
        this.shipsDestroyed, untouchedShips, untouchedBullets.append(newBullets));
  }

  @Override
  public MyGame onKeyEvent(String key) {
    // did we press the space
    Bullet bullet = new Bullet(new MyPosn(this.width / 2, this.height - 40), new MyPosn(0, -8), 2,
        1);
    if (key.equals(" ") && this.bulletsLeft >= 1) {
      return new MyGame(this.width, this.height, this.currentTick, this.bulletsLeft - 1,
          this.shipsDestroyed, this.ships, new ConsLoGamePiece(bullet, this.bullets));
    }
    else {
      return this;
    }
  }

  // Check to see if we need to end the game.
  @Override
  public WorldEnd worldEnds() {
    if (this.bulletsLeft <= 0 && this.bullets.isEmpty()) {
      return new WorldEnd(true, this.makeEndScene());
    }
    else {
      return new WorldEnd(false, this.makeEndScene());
    }
  }

  // makes an end scene
  public WorldScene makeEndScene() {
    WorldScene endScene = new WorldScene(this.width, this.height);
    WorldScene gameOver = endScene.placeImageXY(new TextImage("Game Over", Color.red), 250, 250);
    return gameOver.placeImageXY(
        new TextImage("Ships Destoyed: " + this.shipsDestroyed, Color.black), 250, 275);
  }
}
