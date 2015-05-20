  
  #include <SoftwareSerial.h>
  #include <stdlib.h>
  
  int bluetoothTx = 8;  // TX-O pin of bluetooth mate, Arduino D2
  int bluetoothRx = 9;  // RX-I pin of bluetooth mate, Arduino D3
  
  SoftwareSerial bluetooth(bluetoothTx, bluetoothRx);
  
  int left = 0; 
  int right = 0; 
  int front = 0; 
  
  void setup()
  {
    Serial.begin(9600);  // Begin the serial monitor at 9600bps
  
    bluetooth.begin(115200);  // The Bluetooth Mate defaults to 115200bps
    bluetooth.print("$");  // Print three times individually
    bluetooth.print("$");
    bluetooth.print("$");  // Enter command mode
    delay(100);  // Short delay, wait for the Mate to send back CMD
    bluetooth.println("U,9600,N");  // Temporarily Change the baudrate to 9600, no parity
    // 115200 can be too fast at times for NewSoftSerial to relay the data reliably
    bluetooth.begin(9600);  // Start bluetooth serial at 9600
  }
  
  void loop()
  {
    
    while (true) {
    
      int total = bluetooth.available(); 
    
      if (total > 4) {
        
        char message[total+1];
        
        int i = 0; 
        
        while (bluetooth.available())  {
          
          // Send any characters the bluetooth prints to the serial monitor
          char d = (char)bluetooth.read(); 
          
          message[i] = d; 
          
          if (d != ',') {
              if (i == 0) {
                left = d - '0';
              }
             
              if (i == 2) {
                right = d - '0';
              }
            
              if (i == 4) {
                front = d - '0';
              }    
          }
          
          i++;         
        } 
        
        message[i] = '\0';
        
        Serial.print((String) left);
        Serial.print(",");
        Serial.print((String) right);
        Serial.print(",");
        Serial.println((String) front);
       
        break;
      } 
    }
  }
