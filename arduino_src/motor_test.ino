//front_a : front left
//front_b : front right
//Motor driver IC : L9110S

const int front_a1 = 3;
const int front_a2 = 5;
const int front_b1 = 6;
const int front_b2 = 9;

byte speed = 150;
void setup() {
  pinMode(front_a1, OUTPUT);
  pinMode(front_a2, OUTPUT);
  pinMode(front_b1, OUTPUT);
  pinMode(front_b2, OUTPUT);
}

void backward()
{
  analogWrite(front_a1, speed);
  analogWrite(front_a2, 0);
  analogWrite(front_b1, 0);
  analogWrite(front_b2, speed);
}

void forward()
{
  analogWrite(front_a1, 0);
  analogWrite(front_a2, speed);
  analogWrite(front_b1, speed);
  analogWrite(front_b2, 0);
}

void loop() {
  forward();
  delay(2000);
  backward();
  delay(2000);
}
