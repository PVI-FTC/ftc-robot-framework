{
  "startPoint": {
    "x": 0,
    "y": 0,
    "heading": "linear",
    "startDeg": 90,
    "endDeg": 180,
    "locked": false
  },
  "lines": [
    {
      "id": "line-250ytzc9lq8",
      "name": "Path 1",
      "endPoint": {
        "x": 24,
        "y": 0,
        "heading": "linear",
        "startDeg": 0,
        "endDeg": 0
      },
      "controlPoints": [],
      "color": "#ffc516",
      "locked": false,
      "waitBeforeMs": 0,
      "waitAfterMs": 0,
      "waitBeforeName": "",
      "waitAfterName": ""
    }
  ],
  "shapes": [
    {
      "id": "triangle-1",
      "name": "Red Goal",
      "vertices": [
        { "x": 141.5, "y": 70 },
        { "x": 141.5, "y": 141.5 },
        { "x": 120, "y": 141.5 },
        { "x": 138, "y": 119 },
        { "x": 138, "y": 70 }
      ],
      "color": "#dc2626",
      "fillColor": "#ff6b6b"
    },
    {
      "id": "triangle-2",
      "name": "Blue Goal",
      "vertices": [
        { "x": 6, "y": 119 },
        { "x": 25, "y": 141.5 },
        { "x": 0, "y": 141.5 },
        { "x": 0, "y": 70 },
        { "x": 6, "y": 70 }
      ],
      "color": "#2563eb",
      "fillColor": "#60a5fa"
    }
  ],
  "sequence": [
    { "kind": "path", "lineId": "line-250ytzc9lq8" }
  ],
  "pathChains": [
    {
      "id": "chain-mt1xnrve-qoj7gg",
      "name": "TeamA_LP11_Pilot",
      "color": "#9BB9DD",
      "lineIds": ["line-250ytzc9lq8"]
    }
  ],
  "settings": {
    "xVelocity": 75,
    "yVelocity": 65,
    "aVelocity": 3.141592653589793,
    "kFriction": 0.1,
    "rWidth": 16,
    "rHeight": 16,
    "safetyMargin": 1,
    "maxVelocity": 40,
    "maxAcceleration": 30,
    "maxDeceleration": 30,
    "fieldMap": "decode.webp",
    "robotImage": "/robot.png",
    "theme": "auto",
    "showGhostPaths": false,
    "showOnionLayers": false,
    "onionLayerSpacing": 3,
    "onionColor": "#dc2626",
    "onionNextPointOnly": false,
    "showHeadingArrow": false,
    "headingArrowLength": 50,
    "headingArrowColor": "#ffffff",
    "headingArrowThickness": 2,
    "pathOpacity": 1
  },
  "version": "1.2.1",
  "timestamp": "2026-08-20T19:50:47.809Z"
}
