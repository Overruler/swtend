## SWTend

[xtend](http://xtend-lang.org) libraries for SWT, SWT + Xtend

This project is maintained, however but it is not opend. Currently this project is used by my other projects internal purpose. 
Eventually, When I think APIs are stable enough, I will publish documets about this project.


Here are some hint that explains what **SWTend** is:

### Builder Pattern to create SWT UI
```xtend
var ui = newComposite[
  layout = newGridLayout[
    numColumns = 2
  ]
  
  newTree[
    layoutData = FILL_HORIZONTAL[
      horizontalSpan = 2
    ]
    
    newTreeItem[
      text = "Root"
      
      newTreeItem[text = "Sub"]
      newTreeItem[text = "Sub"]
    ]
    
    newPushButton[
      text = "Push Me!"
      onSelection = [
        println("Hello World!")
      ]
    ]
  ]
]
```
### Geometry Extension
```xtend
  var rect = new Rectangle(0, 0, 10, 10) // SWT Rectangle
  
  rect.contains(5, 5)     // true
  rect.copy()             // clone
  rect.translate(new Point(10, 10))
```


### Custom Controls
* ColorPicker
* ColorWell


### Auto Dispose
```xtend
var shell = ...
var Image img = ...

// img will be disposed when shell is disposed
img.shouldDisposeWith(shell)
// same with "shell.chainDispose(img);"

shell.onPaint = [
  // red will be disposed next event loop
  var red = new Color(display, 255, 0, 0).autoDispose()
  
  gc.background = red;
  gc.fillRectangle(shell.clientArea)
]
