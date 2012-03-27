
int rotz = 0;

void setup()
{
  size(800,600);//,P3D);
//  noLoop();
  PFont fontA = loadFont("courier");
  textFont(fontA, 36);  
}

void draw(){  
  background(125,45,56);
  fill(255);
  stroke(128,255,255,75);
  rotateY(radians(rotz++));
  translate(width/2,height/2);
  line(0,0,0,100,100,100);
  text("Hello Web! "+rotz,20,50);
//  println("Hello ErrorLog!"+rotz);
}