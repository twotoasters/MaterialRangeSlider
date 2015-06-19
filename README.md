#MaterialRangeSlider

![](http://i.imgur.com/2hou4LT.gif)

Material design slider for selecting a range of values.  This is a sample app for this [blog post](http://www.toastdroid.com) explaining the implementation.

MaterialRangeSlider is not in a library.  If you would like to reuse MaterialRangeSlider in your app you will need to copy the MaterialRangeSlider.java file into your project.  You will also need the declaration of the custom attributes in your attrs.xml.  


attrs.xml

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <declare-styleable name="MaterialRangeSlider">
        <attr name="min" format="integer" />
        <attr name="max" format="integer" />
        <attr name="unpressedTargetRadius" format="dimension" />
        <attr name="pressedTargetRadius" format="dimension" />
        <attr name="targetColor" format="color" />
        <attr name="insideRangeLineColor" format="color" />
        <attr name="outsideRangeLineColor" format="color" />
        <attr name="insideRangeLineStrokeWidth" format="dimension" />
        <attr name="outsideRangeLineStrokeWidth" format="dimension" />
    </declare-styleable>
</resources>
```
