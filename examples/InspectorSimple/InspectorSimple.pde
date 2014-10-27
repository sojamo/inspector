import sojamo.inspector.*;

Inspector inspector;

void setup() {
  size(800, 400);
  inspector = new Inspector(this);
  inspector.add("mouseX", "mouseY", "mousePressed");
}

void draw() {
  background(0, 255, 128);
}

