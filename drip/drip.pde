int screen_width = 1024;
int screen_height = 768;

color color_theme[] = { //rainbow
  color(255, 0, 0), //red
  color(255, 128, 0), //orange
  color(255, 255, 0), //yellow
  color(  0, 255, 0), //green 
  color(  0, 255, 128), //cyan?
  color(128, 255, 0), //light green
  color(255, 128, 255), //light purple
  color(255, 0, 255), //purple
  color(  0, 128, 255), //light blue
  color(  0, 0, 255), //blue
};

PImage imgs[] = new PImage[3];

ArrayList hist = new ArrayList();

void setup()
{
  size(screen_width, screen_height);
  background(255);
  smooth();
  for (int i=0;i<3;i++) {
    int imgnum = i%3 +1;
    imgs[i] = loadImage("drip"+imgnum+".png");
  }
}

void showStrike(Strike strike, int i)
{
  PVector v = strike.v;
  float ang = strike.ang;
  float mag = strike.mag;

  tint(color(color_theme[i%color_theme.length]), 200);
  int imgnum = i%3+1;
  PImage img = loadImage("drip"+imgnum+".png");
  img.resize(100+(int)mag, 0);
  showRotatedeImage(img, v.x, v.y, ang);
}

void draw()
{
  background(255);

  for (int i=0;i<hist.size();i++)
  {
    Strike strike = (Strike) hist.get(i);
    showStrike(strike, i);
  }
}

void keyPressed() {
  if (key==' ') {//clear
    background(255);
    hist.clear();
  }
}


PVector pm = new PVector(0, 0);

void mousePressed()
{
  pm = new PVector(mouseX, mouseY);//keep previous mouse location
}

void mouseReleased()
{
  PVector m = new PVector(mouseX, mouseY);
  m.sub(pm);
  float ang = atan2(m.y, m.x);
  PVector v = new PVector(pm.x, pm.y);
  float mag = m.mag();
  Strike strike = new Strike(v, ang, mag);
  hist.add(strike);
}

void showRotatedeImage(PImage img, float x, float y, float ang)
{
  translate(x, y);
  rotate(ang);
  //  imageMode(CENTER);
  image(img, 0, 0);
  rotate(-ang);
  translate(-x, -y);
}

