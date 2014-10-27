import sojamo.inspector.*;


int a = 0;
int b = 0;
float d;
ArrayList l = new ArrayList();
Test test = new Test();

void setup() {
  size(800,400);
  Inspector inspect = new Inspector(this);
  inspect.setSize(320,210).setPosition(10,10).setSpacing(20);
  inspect.add("d","a","mouseX", "mouseY", "mousePressed");
  inspect.add("keyPressed","test values","test values size","frameCount");
  inspect.change("test values", "min",0,"max",50, "scale",0.5);
  }

void draw() {
  background(0,255,128);
  noStroke();
  d = abs(sin(frameCount*0.01)) * 400;
  translate(width/2, height/2);
  ellipse(0,0,(float)d,(float)d);
  test.update();  
}

void mousePressed() {
  a += 1;
  l.add(123);
  l.add(234);
}



class Test {
  ArrayList values = new ArrayList();
  Test() {
    update();
  }
  void update() {
    values.clear();
    for(int i=0;i<140;i++) {
      values.add(random(0,100)/(i*0.1));
    }
  }
  public String toString() {
    return "Testing";
  }
}



