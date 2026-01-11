package logic;

//Enum defines a fixed set of constants (states)
public enum ScreenMode { 
  // State 1: The user can only look at the data (Read-Only)
  VIEW,
  // State 2: The user can change/edit the data
  UPDATE,
  // State 3 : The user can cancel is order
  CANCEL,
  // State 4 : The user can create a new order
  CREATE
  
}

