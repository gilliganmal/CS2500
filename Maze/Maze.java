import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Random;
import tester.*;
import javalib.impworld.*;
import java.awt.Color;
import javalib.worldimages.*;

//to represent a vertex (node)
class Vertex {
  int x;
  int y;
  Color color;
  Vertex top;
  Vertex right;
  Vertex left;
  Vertex bottom;
  boolean topEdge;
  boolean bottomEdge;
  boolean rightEdge;
  boolean leftEdge;
  int size;
  ArrayList<Edge> outEdges = new ArrayList<Edge>(); // adj list

  // the constructor
  Vertex(int x, int y, Color color) {
    this.size = Graph.OFFSET;
    this.x = x * this.size;
    this.y = y * this.size;
    this.color = color;
    this.top = null;
    this.right = null;
    this.left = null;
    this.bottom = null;
    this.topEdge = true;
    this.bottomEdge = true;
    this.rightEdge = true;
    this.leftEdge = true;
  }

  // EFFECT: links to left cell and corrects the right link
  void left(Vertex v) {
    this.left = v;
    v.right = this;
  }

  // EFFECT: links to right cell and corrects the left link
  void right(Vertex v) {
    this.right = v;
    v.left = this;
  }

  // EFFECT: links to top cell and corrects the bottom link
  void top(Vertex v) {
    this.top = v;
    v.bottom = this;
  }

  // EFFECT: links to bottom cell and corrects the top link
  void bottom(Vertex v) {
    this.bottom = v;
    v.top = this;
  }

  // EFFECT: adds an edge between this and the given vertex
  void addEdge(Vertex v, Random random) {
    if (v != null) {
      this.outEdges.add(new Edge(this, v, random));
    }
  }

  // EFFECT: adds an edge between this and bottom and right to outEdges
  void initEdges(Random rand) {
    this.addEdge(this.right, rand);
    this.addEdge(this.bottom, rand);
  }

  // renders this vertex
  WorldImage draw() {
    return new RectangleImage(this.size, this.size, OutlineMode.SOLID, this.color);
  }

  // EFFECT: places this cell on a scene
  void place(WorldScene scene) {
    scene.placeImageXY(this.draw(), this.x + (Graph.WIDTH / (2 * Graph.COLS)),
        this.y + (Graph.HEIGHT / (2 * Graph.ROWS)));
  }

  // EFFECT: places the edges on the scene
  // if edge is true, there is an edge
  void placeEdges(WorldScene scene) {
    if (this.topEdge) {
      WorldImage topEdge = new RectangleImage(this.size, 1, OutlineMode.SOLID, Color.black);
      scene.placeImageXY(topEdge, this.x + (this.size / 2), this.y);
    }
    if (this.bottomEdge) {
      WorldImage bottomEdge = new RectangleImage(this.size, 1, OutlineMode.SOLID, Color.black);
      scene.placeImageXY(bottomEdge, this.x + (this.size / 2), this.y + this.size);
    }
    if (this.leftEdge) {
      WorldImage leftEdge = new RectangleImage(1, this.size, OutlineMode.SOLID, Color.black);
      scene.placeImageXY(leftEdge, this.x, this.y + (this.size / 2));
    }
    if (this.rightEdge) {
      WorldImage rightEdge = new RectangleImage(1, this.size, OutlineMode.SOLID, Color.black);
      scene.placeImageXY(rightEdge, this.x + this.size, this.y + (this.size / 2));
    }
  }
}

//to represent an edge class
class Edge {
  Vertex from;
  Vertex to;
  int weight;
  Random rand;

  // Constructor with seeded random value for testing
  Edge(Vertex from, Vertex to, Random rand) {
    this.from = from;
    this.to = to;
    this.rand = rand;
    this.weight = this.rand.nextInt(10000);
  }

  // the constructor
  Edge(Vertex from, Vertex to) {
    this.from = from;
    this.to = to;
    this.rand = new Random();
    this.weight = this.rand.nextInt(10000);
  }
}

//Compares the weights of Edges
//e2 comes before e1 if positive
class WeightComparator implements Comparator<Edge> {
  public int compare(Edge e1, Edge e2) {
    return e1.weight - e2.weight;
  }
}

//to represent a graph
class Graph extends World {
  static int ROWS;
  static int COLS;
  ArrayList<Vertex> vertices;
  ArrayList<Edge> mst = new ArrayList<Edge>();
  static int OFFSET = 10;
  static int WIDTH;
  static int HEIGHT;
  Random rand;
  HashMap<Vertex, Vertex> cameFromEdge = new HashMap<Vertex, Vertex>();
  ArrayList<Vertex> visited = new ArrayList<Vertex>();
  ArrayDeque<Vertex> worklist = new ArrayDeque<Vertex>();
  ArrayList<Vertex> reconstructList = new ArrayList<Vertex>();
  boolean bfs;
  boolean dfs;

  // the constructor
  Graph(int cols, int rows, Random rand) {
    Graph.ROWS = rows;
    Graph.COLS = cols;
    this.vertices = new ArrayList<Vertex>();
    this.generateVertices();
    this.rand = rand;
    for (Vertex v : this.vertices) {
      v.initEdges(this.rand);
    }
    Graph.WIDTH = Graph.COLS * Graph.OFFSET;
    Graph.HEIGHT = Graph.ROWS * Graph.OFFSET;
    this.mst = this.constructMST();
    this.setEdges();
    this.worklist.add(this.vertices.get(0));
    this.bfs = false;
    this.dfs = false;
  }

  // creates a list of all the edges in a given graph
  ArrayList<Edge> getAllEdges() {
    ArrayList<Edge> allEdges = new ArrayList<Edge>();

    for (Vertex v : vertices) {
      for (Edge e : v.outEdges) {
        allEdges.add(e);
      }
    }
    return allEdges;
  }

  // finds the ancestral representative of a given node
  Vertex find(HashMap<Vertex, Vertex> representatives, Vertex vertex) {
    if (representatives.get(vertex) == vertex) {
      return vertex;
    }
    else {
      return this.find(representatives, representatives.get(vertex));
    }
  }

  // builds a minimum spanning tree
  // edges in the mst represent vertices with no edge between them
  ArrayList<Edge> constructMST() {
    HashMap<Vertex, Vertex> representatives = new HashMap<Vertex, Vertex>();
    ArrayList<Edge> worklist = new ArrayList<Edge>(this.getAllEdges());
    worklist.sort(new WeightComparator());
    ArrayList<Edge> mst = new ArrayList<Edge>();

    // sets vertices and adds edges
    for (Vertex v : this.vertices) {
      representatives.put(v, v);
    }

    while (!worklist.isEmpty()) {
      Edge e = worklist.remove(0); // get first from the worklist and remove it
      // ancestral representative of from vertex
      Vertex fromRep = this.find(representatives, e.from);
      Vertex toRep = this.find(representatives, e.to); // ancestral representative of to vertex
      if (fromRep != toRep) { // if they do not have the same representative
        mst.add(e); // add to the minimum spanning tree
        representatives.replace(this.find(representatives, toRep), fromRep); // union
      }
    }
    return mst;
  }

  // EFFECT: generates all the cells in a board
  void generateVertices() {
    for (int row = 0; row < Graph.ROWS; row++) {
      for (int col = 0; col < Graph.COLS; col++) {
        Vertex v = new Vertex(col, row, Color.LIGHT_GRAY);
        if (row == 0 && col == 0) {
          v.color = Color.GREEN;
          this.vertices.add(v);
        }
        else if (this.vertices.size() == Graph.ROWS * Graph.COLS - 1) {
          v.color = Color.MAGENTA;
          v.left(this.vertices.get(this.vertices.size() - 1));
          v.top(this.vertices.get(this.vertices.size() - Graph.COLS));
          this.vertices.add(v);
        }
        else if (row == 0) {
          v.left(this.vertices.get(this.vertices.size() - 1));
          this.vertices.add(v);
        }
        else if (col == 0) {
          v.top(this.vertices.get(this.vertices.size() - Graph.COLS));
          this.vertices.add(v);
        }
        else {
          v.left(this.vertices.get(this.vertices.size() - 1));
          v.top(this.vertices.get(this.vertices.size() - Graph.COLS));
          this.vertices.add(v);
        }
      }
    }
  }

  // EFFECT: sets all the edges of the minimum spanning tree to false
  void setEdges() {
    for (Edge e : this.mst) {
      if (e.from.top == e.to) {
        e.from.topEdge = false;
        e.to.bottomEdge = false;
      }
      if (e.from.bottom == e.to) {
        e.from.bottomEdge = false;
        e.to.topEdge = false;
      }
      if (e.from.right == e.to) {
        e.from.rightEdge = false;
        e.to.leftEdge = false;
      }
      if (e.from.left == e.to) {
        e.from.leftEdge = false;
        e.to.rightEdge = false;
      }
    }
  }

  // to be called on tick
  // takes the next step to solving the maze
  void solve(Vertex v) {
    if (this.visited.contains(v)) {
      // do nothing
    }
    else if (v == this.vertices.get(this.vertices.size() - 1)) {
      this.worklist.clear();
      this.reconstructList(v);
    }
    else {
      this.visited.add(v);
      v.color = new Color(150, 180, 255);
      this.addNeighbors(v);
    }
  }

  // adds the neighbors of the given vertex and records the edge
  void addNeighbors(Vertex v) {
    if (!v.topEdge) {
      this.addNeighbor(v, v.top);
    }
    if (!v.rightEdge) {
      this.addNeighbor(v, v.right);
    }
    if (!v.bottomEdge) {
      this.addNeighbor(v, v.bottom);
    }
    if (!v.leftEdge) {
      this.addNeighbor(v, v.left);
    }
  }

  // adds a single neighbor
  void addNeighbor(Vertex v, Vertex neighbor) {
    if (!this.visited.contains(neighbor)) {
      if (this.bfs) {
        this.worklist.add(neighbor);
      }
      else {
        this.worklist.addFirst(neighbor);
      }
      this.cameFromEdge.put(neighbor, v);
    }
  }

  // generates a reconstruct list
  void reconstructList(Vertex v) {
    Vertex from = this.cameFromEdge.get(v);
    if (v != this.vertices.get(0)) {
      this.reconstructList.add(v);
      this.reconstructList(from);
    }
  }

  // reconstructs the maze following the hashmap
  void reconstruct() {
    Vertex v = this.reconstructList.remove(0);
    v.color = Color.blue;
    if (this.reconstructList.isEmpty()) {
      this.vertices.get(0).color = Color.blue;
    }
  }

  @Override
  // updates the maze solution on tick
  public void onTick() {
    if (this.bfs || this.dfs) {
      if (!this.worklist.isEmpty()) {
        Vertex v = worklist.remove();
        this.solve(v);
      }
    }
    if (!this.reconstructList.isEmpty()) {
      this.reconstruct();
    }
  }

  // handles key events
  // b - breadth first search
  // d - depth first search
  // r - reset
  @Override
  public void onKeyEvent(String key) {
    if (key.equals("b")) {
      this.bfs = true;
    }
    if (key.equals("d")) {
      this.dfs = true;
    }
    if (key.equals("r")) {
      this.vertices = new ArrayList<Vertex>();
      generateVertices();
      for (Vertex v : this.vertices) {
        v.initEdges(this.rand);
      }
      this.mst = this.constructMST();
      this.setEdges();
      this.cameFromEdge = new HashMap<Vertex, Vertex>();
      this.visited = new ArrayList<Vertex>();
      this.worklist = new ArrayDeque<Vertex>();
      this.worklist.add(this.vertices.get(0));
      this.bfs = false;
      this.dfs = false;
    }
  }

  @Override
  // makes the world scene that is displayed
  public WorldScene makeScene() {
    WorldScene scene = new WorldScene(Graph.COLS * Graph.OFFSET, Graph.ROWS * Graph.OFFSET);
    return this.placeEdges(this.placeGrid(scene));
  }

  // places grid on the screen
  public WorldScene placeGrid(WorldScene scene) {
    for (Vertex v : this.vertices) {
      v.place(scene);
    }
    return scene;
  }

  // places edges on the scene
  public WorldScene placeEdges(WorldScene scene) {
    for (Edge e : this.mst) {
      e.from.placeEdges(scene);
      e.to.placeEdges(scene);
    }
    return scene;
  }
}

//to represent examples and tests of MyWorldProgram
class ExamplesMazeWorld {
  void testGame(Tester t) {
    Graph g = new Graph(100, 60, new Random());
    g.bigBang(Graph.COLS * Graph.OFFSET + 2, Graph.ROWS * Graph.OFFSET + 2, 0.0001);
  }
}

// an Examples class for all parts of the maze
class ExamplesMaze {
  ExamplesMaze() {
  }

  // Vertices
  Vertex v1;
  Vertex v2;
  Vertex v3;
  Vertex v4;

  // Edges
  Edge e1;
  Edge e2;
  Edge e3;
  Edge e4;

  // Graphs
  Graph g0;
  Graph g1;
  Graph g2;
  Graph g3;

  // function classes
  WeightComparator wc;
  HashMap<Vertex, Vertex> hm;

  // WorldScenes
  WorldScene background;
  WorldScene background2;

  // Array lists of edges and vertices
  ArrayList<Edge> edges1;
  ArrayList<Edge> edges2;
  ArrayList<Edge> edges3;
  ArrayList<Edge> sortedges;
  ArrayList<Vertex> vertex1;
  ArrayList<Vertex> vertex2;
  ArrayList<Vertex> vertex3;
  ArrayList<Vertex> vertex4;

  // data to be used specifically for testing
  void init() {
    this.v1 = new Vertex(0, 0, Color.GREEN);
    this.v2 = new Vertex(1, 0, Color.LIGHT_GRAY);
    this.v3 = new Vertex(0, 1, Color.LIGHT_GRAY);
    this.v4 = new Vertex(1, 1, Color.magenta);

    this.e1 = new Edge(v1, v2);
    this.e2 = new Edge(v1, v3);
    this.e3 = new Edge(v2, v4);
    this.e4 = new Edge(v3, v4);
    this.e1.weight = 0;
    this.e2.weight = 1;
    this.e3.weight = 0;
    this.e4.weight = 2;

    this.g0 = new Graph(2, 2, new Random(5));
    this.g1 = new Graph(10, 10, new Random(5));
    this.g2 = new Graph(50, 25, new Random(5));
    this.g3 = new Graph(100, 60, new Random(5));

    this.wc = new WeightComparator();
    this.hm = new HashMap<Vertex, Vertex>(4, 1);
    this.background = new WorldScene(500, 500);
    this.background2 = new WorldScene(500, 500);

    this.edges1 = new ArrayList<Edge>(Arrays.asList(this.e1));
    this.edges2 = new ArrayList<Edge>(Arrays.asList(this.e1, this.e2));
    this.edges3 = new ArrayList<Edge>(Arrays.asList(this.e1, this.e2, this.e3, this.e4));
    this.sortedges = new ArrayList<Edge>(Arrays.asList(this.e1, this.e3, this.e2, this.e4));
    this.vertex1 = new ArrayList<Vertex>(Arrays.asList(this.v1));
    this.vertex2 = new ArrayList<Vertex>(Arrays.asList(this.v1, this.v2));
    this.vertex3 = new ArrayList<Vertex>(Arrays.asList(this.v1, this.v2, this.v3));
    this.vertex4 = new ArrayList<Vertex>(Arrays.asList(this.v1, this.v2, this.v3, this.v4));
  }

  // to test the Left method in Vertex class
  void testLeft(Tester t) {
    this.init();
    this.v2.left(this.v1);
    t.checkExpect(this.v2.left, this.v1);
    t.checkExpect(this.v1.right, this.v2);
    this.v4.left(this.v3);
    t.checkExpect(this.v4.left, this.v3);
    t.checkExpect(this.v3.right, this.v4);
  }

  // to test the Right method in Vertex class
  void testRight(Tester t) {
    this.init();
    this.v1.right(this.v2);
    t.checkExpect(this.v2.left, this.v1);
    t.checkExpect(this.v1.right, this.v2);
    this.v3.right(this.v4);
    t.checkExpect(this.v4.left, this.v3);
    t.checkExpect(this.v3.right, this.v4);
  }

  // to test the Top method in Vertex class
  void testTop(Tester t) {
    this.init();
    this.v3.top(this.v1);
    t.checkExpect(this.v3.top, this.v1);
    t.checkExpect(this.v1.bottom, this.v3);
    this.v4.top(this.v2);
    t.checkExpect(this.v4.top, this.v2);
    t.checkExpect(this.v2.bottom, this.v4);
  }

  // to test the Bottom method in Vertex class
  void testBottom(Tester t) {
    this.init();
    this.v1.bottom(this.v3);
    t.checkExpect(this.v3.top, this.v1);
    t.checkExpect(this.v1.bottom, this.v3);
    this.v2.bottom(this.v4);
    t.checkExpect(this.v4.top, this.v2);
    t.checkExpect(this.v2.bottom, this.v4);
  }

  // to test the addEdge method in the Vertex class
  void testAddEdge(Tester t) {
    this.init();
    t.checkExpect(this.v1.outEdges, new ArrayList<Edge>());
    t.checkExpect(this.v2.outEdges, new ArrayList<Edge>());
    this.v1.addEdge(v2, new Random(7));
    this.v1.right = this.v2;
    this.v1.initEdges(new Random(7));
    Vertex v = new Vertex(0, 0, Color.green);
    Vertex v1 = new Vertex(1, 0, Color.LIGHT_GRAY);
    v.right = v1;
    Edge e = new Edge(v, v1);
    e.weight = 4236;
    v.outEdges = new ArrayList<Edge>(Arrays.asList(e, e));
    t.checkExpect(this.v2.outEdges, new ArrayList<Edge>());
    t.checkExpect(this.v1.outEdges, new ArrayList<Edge>(Arrays.asList(e, e)));
  }

  // to test the InitEdges method in the Vertex class
  void testInitEdges(Tester t) {
    this.init();
    this.v1.right = this.v2;
    t.checkExpect(this.v1.outEdges, new ArrayList<Edge>());
    this.v1.initEdges(new Random(7));
    Vertex v = new Vertex(0, 0, Color.green);
    Vertex v1 = new Vertex(1, 0, Color.LIGHT_GRAY);
    v.right = v1;
    Edge e = new Edge(v, v1);
    e.weight = 4236;
    v.outEdges = new ArrayList<Edge>(Arrays.asList(e));
    t.checkExpect(this.v1.outEdges, new ArrayList<Edge>(Arrays.asList(e)));
  }

  // to test the draw method in the vertex class
  void testDraw(Tester t) {
    this.init();
    t.checkExpect(this.v1.draw(), new RectangleImage(10, 10, OutlineMode.SOLID, Color.green));
    t.checkExpect(this.v2.draw(), new RectangleImage(10, 10, OutlineMode.SOLID, Color.LIGHT_GRAY));
    t.checkExpect(this.v4.draw(), new RectangleImage(10, 10, OutlineMode.SOLID, Color.magenta));
  }

  // to test the place method in the Vertex class
  void testPlace(Tester t) {
    this.init();
    this.background = new WorldScene(500, 500);
    this.v1.place(background);
    this.background2.placeImageXY(this.v1.draw(), this.v1.x + (Graph.WIDTH / (2 * Graph.COLS)),
        this.v1.y + (Graph.HEIGHT / (2 * Graph.ROWS)));
    t.checkExpect(this.background, this.background2);
  }

  // to test the placeEdges method in the WeightComparator class
  void testCompare(Tester t) {
    this.init();
    t.checkExpect(this.wc.compare(this.e1, this.e2), -1);
    t.checkExpect(this.wc.compare(this.e2, this.e1), 1);
    t.checkExpect(this.wc.compare(this.e4, this.e1), 2);
    t.checkExpect(this.wc.compare(this.e1, this.e1), 0);
    t.checkExpect(this.wc.compare(this.e1, this.e3), 0);
  }

  // to test the getAllEdges method in the graph class
  void testGetAllEdges(Tester t) {
    this.init();
    this.v1.outEdges = this.edges1;
    this.g1.vertices = this.vertex1;
    this.v2.outEdges = this.edges2;
    this.g2.vertices = this.vertex2;
    this.v3.outEdges = this.edges3;
    this.g3.vertices = this.vertex3;
    t.checkExpect(this.g1.getAllEdges(), this.edges1);
    t.checkExpect(this.g2.getAllEdges(),
        new ArrayList<Edge>(Arrays.asList(this.e1, this.e1, this.e2)));
    t.checkExpect(this.g3.getAllEdges(), new ArrayList<Edge>(
        Arrays.asList(this.e1, this.e1, this.e2, this.e1, this.e2, this.e3, this.e4)));
  }

  // to test the Find method in the Graph class
  void testFind(Tester t) {
    this.init();
    t.checkExpect(this.g0.find(this.hm, this.g0.vertices.get(0)), null);
    this.hm.put(this.g0.vertices.get(0), this.g0.vertices.get(0));
    this.hm.put(this.g0.vertices.get(1), this.g0.vertices.get(1));
    this.hm.put(this.g0.vertices.get(2), this.g0.vertices.get(2));
    this.hm.put(this.g0.vertices.get(3), this.g0.vertices.get(3));
    t.checkExpect(this.g0.find(this.hm, this.g0.vertices.get(0)), this.g0.vertices.get(0));
    t.checkExpect(this.g0.find(this.hm, this.g0.vertices.get(1)), this.g0.vertices.get(1));
    t.checkExpect(this.g0.find(this.hm, this.g0.vertices.get(2)), this.g0.vertices.get(2));
  }

  // to test the constructMST method in the graph class
  void testConstructMST(Tester t) {
    this.init();
    HashMap<Vertex, Vertex> representatives = new HashMap<Vertex, Vertex>();
    ArrayList<Edge> worklist = new ArrayList<Edge>(this.g0.getAllEdges());
    worklist.sort(new WeightComparator());
    ArrayList<Edge> mst = new ArrayList<Edge>();

    for (Vertex v : this.g0.vertices) {
      representatives.put(v, v);
    }

    while (!worklist.isEmpty()) {
      Edge e = worklist.remove(0);
      Vertex fromRep = this.g0.find(representatives, e.from);
      Vertex toRep = this.g0.find(representatives, e.to);
      if (fromRep != toRep) {
        mst.add(e);
        representatives.replace(this.g0.find(representatives, toRep), fromRep);
      }
    }
    t.checkExpect(this.g0.constructMST(), mst);
    t.checkExpect(this.g0.mst, mst);
  }

  // to test the generateVertices method in the graph class
  void testGenerateVertices(Tester t) {
    this.init();
    Graph graph = new Graph(2, 2, new Random(5));
    graph.generateVertices();
    t.checkExpect(this.g0.vertices.get(0).top, null);
    t.checkExpect(this.g0.vertices.get(0).left, null);
    t.checkExpect(this.g0.vertices.get(0).right, graph.vertices.get(1));
    t.checkExpect(this.g0.vertices.get(0).bottom, graph.vertices.get(2));
    t.checkExpect(this.g0.vertices.get(1).top, null);
    t.checkExpect(this.g0.vertices.get(1).left, graph.vertices.get(0));
    t.checkExpect(this.g0.vertices.get(1).right, null);
    t.checkExpect(this.g0.vertices.get(1).bottom, graph.vertices.get(3));
    t.checkExpect(this.g0.vertices.get(2).top, graph.vertices.get(0));
    t.checkExpect(this.g0.vertices.get(2).left, null);
    t.checkExpect(this.g0.vertices.get(2).right, graph.vertices.get(3));
    t.checkExpect(this.g0.vertices.get(2).bottom, null);
    t.checkExpect(this.g0.vertices.get(3).top, graph.vertices.get(1));
    t.checkExpect(this.g0.vertices.get(3).left, graph.vertices.get(2));
    t.checkExpect(this.g0.vertices.get(3).right, null);
    t.checkExpect(this.g0.vertices.get(3).bottom, null);
  }

  // to test the setEdges method in the graph class
  void testSetEdges(Tester t) {
    this.init();
    this.g0.setEdges();
    t.checkExpect(this.g0.mst.get(0).from.topEdge, true);
    t.checkExpect(this.g0.mst.get(0).from.bottomEdge, false);
    t.checkExpect(this.g0.mst.get(0).from.leftEdge, true);
    t.checkExpect(this.g0.mst.get(0).from.rightEdge, true);
  }

  // to test the solve method in the graph class
  void testSolve(Tester t) {
    this.init();
    this.g0.vertices = new ArrayList<Vertex>(Arrays.asList(this.v1));
    this.g0.worklist = new ArrayDeque<Vertex>(Arrays.asList(this.v1, this.v2));
    this.g0.solve(v1);
    t.checkExpect(this.g0.worklist, new ArrayDeque<Vertex>(Arrays.asList()));
    t.checkExpect(this.v2.color, Color.LIGHT_GRAY);
    this.g0.vertices = new ArrayList<Vertex>(Arrays.asList(this.v1));
    this.g0.worklist = new ArrayDeque<Vertex>(Arrays.asList(this.v1, this.v2));
    this.g0.solve(v2);
    t.checkExpect(this.v2.color, new Color(150, 180, 255));
  }

  // to test the addNeighbors method in the graph class;
  void testAddNeighbors(Tester t) {
    this.init();
    g0.worklist = new ArrayDeque<Vertex>();
    this.g0.worklist.add(this.v4);
    this.v2.bottomEdge = true;
    this.v2.rightEdge = true;
    this.v2.leftEdge = true;
    this.v2.topEdge = false;
    this.v2.top = this.v1;
    this.g0.addNeighbors(this.v2);
    t.checkExpect(this.g0.worklist, new ArrayDeque<Vertex>(Arrays.asList(this.v1, this.v4)));
    this.v2.leftEdge = false;
    this.v2.topEdge = true;
    this.g0.bfs = true;
    this.v2.left = this.v1;
    this.g0.addNeighbors(this.v2);
    t.checkExpect(this.g0.worklist,
        new ArrayDeque<Vertex>(Arrays.asList(this.v1, this.v4, this.v1)));
    this.v2.leftEdge = true;
    this.v2.rightEdge = false;
    this.v2.right = this.v1;
    this.g0.addNeighbors(this.v2);
    t.checkExpect(this.g0.worklist,
        new ArrayDeque<Vertex>(Arrays.asList(this.v1, this.v4, this.v1, this.v1)));
    this.g0.visited = new ArrayList<Vertex>(Arrays.asList(this.v1));
    this.v2.leftEdge = true;
    this.v2.topEdge = false;
    this.v2.top = this.v1;
    this.g0.addNeighbors(this.v2);
    t.checkExpect(this.g0.worklist,
        new ArrayDeque<Vertex>(Arrays.asList(this.v1, this.v4, this.v1, this.v1)));
    this.g0.visited = new ArrayList<Vertex>(Arrays.asList());
    this.g0.addNeighbors(this.v2);
    t.checkExpect(this.g0.worklist, new ArrayDeque<Vertex>(
        Arrays.asList(this.v1, this.v4, this.v1, this.v1, this.v1, this.v1)));
  }

  // to test the addNeighbor method in the graph class
  void testAddNeighbor(Tester t) {
    this.init();
    g0.worklist = new ArrayDeque<Vertex>();
    this.g0.worklist.add(this.v4);
    this.g0.addNeighbor(this.v2, this.v1);
    t.checkExpect(this.g0.worklist, new ArrayDeque<Vertex>(Arrays.asList(this.v1, this.v4)));
    this.g0.bfs = true;
    this.g0.addNeighbor(this.v2, this.v1);
    t.checkExpect(this.g0.worklist,
        new ArrayDeque<Vertex>(Arrays.asList(this.v1, this.v4, this.v1)));
    this.g0.visited = new ArrayList<Vertex>(Arrays.asList(this.v1));
    this.g0.addNeighbor(this.v2, this.v1);
    t.checkExpect(this.g0.worklist,
        new ArrayDeque<Vertex>(Arrays.asList(this.v1, this.v4, this.v1)));
  }

  // to test the reconstructList method in the graph class
  void testReconstructList(Tester t) {
    this.init();
    t.checkExpect(this.g0.reconstructList, new ArrayList<Vertex>());
    this.g0.vertices = new ArrayList<Vertex>(Arrays.asList(this.v1));
    this.g0.reconstructList(this.v1);
    this.g0.cameFromEdge.put(v1, v1);
    t.checkExpect(this.g0.reconstructList, new ArrayList<Vertex>(Arrays.asList()));
    this.g0.vertices = new ArrayList<Vertex>(Arrays.asList(this.v1));
    this.g0.cameFromEdge.put(v2, v1);
    this.g0.reconstructList(this.v2);
    t.checkExpect(this.g0.reconstructList, new ArrayList<Vertex>(Arrays.asList(this.v2)));
  }

  // to test the reconstruct method in the graph class
  void testReconstruct(Tester t) {
    this.init();
    this.g0.vertices = new ArrayList<Vertex>(Arrays.asList(this.v1));
    this.g0.reconstructList = new ArrayList<Vertex>(Arrays.asList(this.v1, this.v1));
    t.checkExpect(this.v1.color, Color.green);
    this.g0.reconstruct();
    t.checkExpect(this.v1.color, Color.blue);
  }

  // to test the onTick method in the graph class
  void testOnTick(Tester t) {
    this.init();
    this.g0.bfs = true;
    this.g0.vertices = new ArrayList<Vertex>(Arrays.asList(this.v1));
    this.g0.worklist = new ArrayDeque<Vertex>(Arrays.asList(this.v1, this.v2));
    this.g0.onTick();
    t.checkExpect(this.g0.worklist, new ArrayDeque<Vertex>(Arrays.asList()));
    this.g0.vertices = new ArrayList<Vertex>(Arrays.asList(this.v1));
    this.g0.reconstructList = new ArrayList<Vertex>(Arrays.asList(this.v1, this.v1));
    t.checkExpect(this.v1.color, Color.green);
    this.g0.onTick();
    t.checkExpect(this.v1.color, Color.blue);
  }

  // to test the onKeyEvent method in the graph class
  void testOnKeyEvent(Tester t) {
    t.checkExpect(this.g1.bfs, false);
    this.g1.onKeyEvent("b");
    t.checkExpect(this.g1.bfs, true);
    t.checkExpect(this.g0.dfs, false);
    this.g0.onKeyEvent("d");
    t.checkExpect(this.g0.dfs, true);
    this.g0.onKeyEvent("r");
    t.checkExpect(this.g0.dfs, false);
  }

  // to test the makeScene method in the graph class
  void testMakeScene(Tester t) {
    this.init();
    WorldScene scene = new WorldScene(Graph.COLS * Graph.OFFSET, Graph.ROWS * Graph.OFFSET);
    t.checkExpect(this.g1.makeScene(), this.g1.placeEdges(this.g1.placeGrid(scene)));
    t.checkExpect(this.g2.makeScene(), this.g2.placeEdges(this.g2.placeGrid(scene)));
  }

  // to test the placeGrid method in the graph class
  void testPlaceGrid(Tester t) {
    this.init();
    t.checkExpect(this.background, new WorldScene(500, 500));
    this.g1.placeGrid(this.background);
    for (Vertex v : this.g1.vertices) {
      v.place(this.background2);
    }
    t.checkExpect(this.background, this.background2);
  }

  // to test the placeEdges method in the graph and vertex class
  void testPlaceEdges(Tester t) {
    this.init();
    t.checkExpect(this.background, new WorldScene(500, 500));
    this.g1.placeEdges(this.background);
    for (Edge e : this.g1.mst) {
      e.from.placeEdges(this.background2);
      e.to.placeEdges(this.background2);
    }
    t.checkExpect(this.background, this.background2);

    // top edge
    this.init();
    this.v1.topEdge = true;
    this.v1.bottomEdge = false;
    this.v1.leftEdge = false;
    this.v1.rightEdge = false;
    this.v1.placeEdges(this.background);
    WorldImage topEdge = new RectangleImage(this.v1.size, 1, OutlineMode.SOLID, Color.black);
    this.background2.placeImageXY(topEdge, this.v1.x + (this.v1.size / 2), this.v1.y);
    t.checkExpect(this.background, this.background2);

    // bottom edge
    this.init();
    this.v1.topEdge = false;
    this.v1.bottomEdge = true;
    this.v1.leftEdge = false;
    this.v1.rightEdge = false;
    this.v1.placeEdges(this.background);
    WorldImage bottomEdge = new RectangleImage(this.v1.size, 1, OutlineMode.SOLID, Color.black);
    this.background2.placeImageXY(bottomEdge, this.v1.x + (this.v1.size / 2),
        this.v1.y + this.v1.size);
    t.checkExpect(this.background, this.background2);

    // left edge
    this.init();
    this.v1.topEdge = false;
    this.v1.bottomEdge = false;
    this.v1.leftEdge = true;
    this.v1.rightEdge = false;
    this.v1.placeEdges(this.background);
    WorldImage leftEdge = new RectangleImage(1, this.v1.size, OutlineMode.SOLID, Color.black);
    this.background2.placeImageXY(leftEdge, this.v1.x, this.v1.y + (this.v1.size / 2));
    t.checkExpect(this.background, this.background2);

    // right edge
    this.init();
    this.v1.topEdge = false;
    this.v1.bottomEdge = false;
    this.v1.leftEdge = false;
    this.v1.rightEdge = true;
    this.v1.placeEdges(this.background);
    WorldImage rightEdge = new RectangleImage(1, this.v1.size, OutlineMode.SOLID, Color.black);
    this.background2.placeImageXY(rightEdge, this.v1.x + this.v1.size,
        this.v1.y + (this.v1.size / 2));
    t.checkExpect(this.background, this.background2);
  }
}