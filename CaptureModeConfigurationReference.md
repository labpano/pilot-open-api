Capture Mode Configuration Reference

# Photo

## Normal Photo(np)

### Basic parameters

| **function** | **Value** | **Reference Api** |
|:-|:-|:-|
| Steady | false：Off、true：On(default)。 | com.pi.pano.PilotSDK\#useGyroscope |
| Resolution | \_5.7K：5760X2880(default) 。 | com.pi.pano.PilotSDK\#takePhoto |

### Pro parameters

| **function** | **Value** | **Reference Api** |
|:-|:-|:-|
| Expose Time Mode | 0：Auto(default)；1：Manual。 | - |
| Expose Time | 0(default)、3200、2000、1000、500、250、100、60、15、3。 | com.pi.pano.PilotSDK\#setExposeTime |
| Exposure Compensation | -4、-3、-2、-1、0(default)、1、2、3、4。 | com.pi.pano.PilotSDK\#setExposureCompensation |
| Sensitivity in Auto Expose Time Mode | 0(default)。 | com.pi.pano.PilotSDK\#setISO |
| Sensitivity in Manual Expose Time Mode | 100、200、400、600、800(default) 、1000、1600、3200、6400。 | com.pi.pano.PilotSDK\#setISO |
| White Balance | auto：Auto(default)；daylight；cloudy-daylight；incandescent；fluorescent。 | com.pi.pano.PilotSDK\#setWhiteBalance |

## Street View Photo(stp)

### Basic parameters

| **function** | **Value** | **Reference Api** |
|:-|:-|:-|
| Steady | false：Off；true：On(default)。 | com.pi.pano.PilotSDK\#useGyroscope |
| Resolution | \_5.7K：5760X2880(default)。 | com.pi.pano.PilotSDK\#takePhoto |

### Pro parameters

| **function** | **Value** | **Reference Api** |
|:-|:-|:-|
| Exposure Time Mode | 0：Auto(default)；1：Manual。 | - |
| Exposure Time | 0(default)、3200、2000、1000、500、250、100、60、15、3。 | com.pi.pano.PilotSDK\#setExposeTime |
| Exposure Compensation | -4、-3、-2、-1、0(default)、1、2、3、4。 | com.pi.pano.PilotSDK\#setExposureCompensation |
| Sensitivity in Auto Expose Time Mode | 0(default)。 | com.pi.pano.PilotSDK\#setISO |
| Sensitivity in Manual Expose Time Mode | 100、200、400、600、800(default) 、1000、1600、3200、6400。 | com.pi.pano.PilotSDK\#setISO |
| White Balance | auto：Auto(default)；daylight；cloudy-daylight；incandescent；fluorescent。 | com.pi.pano.PilotSDK\#setWhiteBalance |

## Tour Photo(tour)

### Basic parameters

| **function** | **Value** | **Reference Api** |
|:-|:-|:-|
| Steady | false：Off；true：On(default)。 | com.pi.pano.PilotSDK\#useGyroscope |
| Resolution | \_5.7K：5760X2880(default)。 | com.pi.pano.PilotSDK\#takePhoto |

### Pro parameters

| **function** | **Value** | **Reference Api** |
|:-|:-|:-|
| Exposure Time Mode | 0：Auto(default)；1：Manual。 | - |
| Exposure Time | 0(default)、3200、2000、1000、500、250、100、60、15、3。 | com.pi.pano.PilotSDK\#setExposeTime |
| Exposure Compensation | -4、-3、-2、-1、0(default) 、1、2、3、4。 | com.pi.pano.PilotSDK\#setExposureCompensation |
| Sensitivity in Auto Expose Time Mode | 0(default)。 | com.pi.pano.PilotSDK\#setISO |
| Sensitivity in Manual Expose Time Mode | 100、200、400、600、800(default) 、1000、1600、3200、6400。 | com.pi.pano.PilotSDK\#setISO |
| White Balance | auto：Auto(default)、daylight、cloudy-daylight；incandescent；fluorescent。 | com.pi.pano.PilotSDK\#setWhiteBalance |

# Video

## Normal Video(nv_u)

### Basic parameters

| **function** | **Value** | **Reference Api** |
|:-|:-|:-|
| Steady | false：Off；true：On(default)。 | com.pi.pano.PilotSDK\#useGyroscope |
| Resolution | \_5.7K：5760X2880(default)； \_4K：3840X1920； \_2.5K：2560X1280。 | com.pi.pano.PilotSDK\#startRecord |
| Encode | H.264(default)、H.265。 | com.pi.pano.PilotSDK\#startRecord |
| Frame Rate | \_5.7K：30(default)，25，24； \_4K：60(default)，30，25，24； \_2.5K：110，90(default)。 | com.pi.pano.PilotSDK\#startRecord |
| Steady Orientation | fix：Fixed(default)；follow：Follow camera orientation。 | - |

### Pro parameters

| **function** | **Value** | **Reference Api** |
|:-|:-|:-|
| Exposure Time Mode | 0：Auto(default);1：Manual。 | - |
| Exposure Time | 0(default)、3200、2000、1000、500、250、120。 | com.pi.pano.PilotSDK\#setExposeTime |
| Exposure Compensation | -4、-3、-2、-1、0(default)、1、2、3、4。 | com.pi.pano.PilotSDK\#setExposureCompensation |
| Sensitivity in Auto Expose Time Mode | 0(default)。 | com.pi.pano.PilotSDK\#setISO |
| Sensitivity in Manual Expose Time Mode | 100、200、400、600、800(default)、1000、1600、3200、6400。 | com.pi.pano.PilotSDK\#setISO |
| White Balance | auto：Auto(default)；daylight；cloudy-daylight；incandescent；fluorescent。 | com.pi.pano.PilotSDK\#setWhiteBalance |

## Plane Video(plv)

### Basic parameters

| **function** | **Value** | **Reference Api** |
|:-|:-|:-|
| Steady | false：Off；true：On(default)。 | com.pi.pano.PilotSDK\#useGyroscope |
| Encode | H.264(default)、H.265。 | com.pi.pano.PilotSDK\#startRecord |
| Resolution | Related to field angle and picture proportion。<br/>***Field Angle：150°***<br/>9:16：【1280\*2276、1080\*1920(default)、720\*1280】；<br/>16:9：【2276\*1280、1920\*1080、1280\*720】；<br/>1:1：【1920\*1920、1440\*1440、1280\*1280、1080\*1080、720\*720】。<br/>***Field Angle：135°***<br/>9:16：【1080\*1920、720\*1280】；<br/>16:9：【1920\*1080、1280\*720】；<br/>1:1：【1920\*1920、1440\*1440、1280\*1280、1080\*1080、720\*720】。<br/>***Field Angle：120°***<br/>9:16：【1080\*1920、720\*1280】；<br/>16:9：【1920\*1080、1280\*720】；<br/>1:1：【1920\*1920、1440\*1440、1280\*1280、1080\*1080、720\*720】。<br/>***Field Angle：90°***<br/>9:16：【720\*1280】；<br/>16:9：【1280\*720】；<br/>1:1：【1440\*1440、1280\*1280、1080\*1080、720\*720】。 | com.pi.pano.PilotSDK\#startRecord |
| Frame Rate | 60(default)，30，25，24。 | com.pi.pano.PilotSDK\#startRecord |
| Steady Orientation | fix：Fixed；follow：Follow camera orientation(default)。 | - |

### Pro parameters

| **function** | **Value** | **Reference Api** |
|:-|:-|:-|
| Exposure Time Mode | 0：Auto(default)；1：Manual。 | - |
| Exposure Time | 0(default)、3200、2000、1000、500、250、120。 | com.pi.pano.PilotSDK\#setExposeTime |
| Exposure Compensation | -4、-3、-2、-1、0(default)、1、2、3、4。 | com.pi.pano.PilotSDK\#setExposureCompensation |
| Sensitivity in Auto Expose Time Mode | 0(default)。 | com.pi.pano.PilotSDK\#setISO |
| Sensitivity in Manual Expose Time Mode | 100、200、400、600、800(default) 、1000、1600、3200、6400。 | com.pi.pano.PilotSDK\#setISO |
| White Balance | auto：Auto(default)；daylight；cloudy-daylight；incandescent；fluorescent。 | com.pi.pano.PilotSDK\#setWhiteBalance |

## Slow Motion Video(smv)

### Basic parameters

| **function** | **Value** | **Reference Api** |
|:-|:-|:-|
| Steady | false：Off；true：On(default)。 | com.pi.pano.PilotSDK\#useGyroscope |
| Resolution | \_2.5K：2560X1280(default)。 | com.pi.pano.PilotSDK\#startRecord |
| Encode | H.264(default)、H.265。 | com.pi.pano.PilotSDK\#startRecord |
| Frame Rate | 110(default)。 | com.pi.pano.PilotSDK\#startRecord |
| Steady Orientation | fix：Fixed(default)；follow：Follow camera orientation。 | - |

### Pro parameters

| **function** | **Value** | **Reference Api** |
|:-|:-|:-|
| Exposure Time Mode | 0：Auto(default)、1：Manual。 | - |
| Exposure Time | 0(default)、3200、2000、1000、500、250、120。 | com.pi.pano.PilotSDK\#setExposeTime |
| Exposure Compensation | -4、-3、-2、-1、0(default)、1、2、3、4。 | com.pi.pano.PilotSDK\#setExposureCompensation |
| Sensitivity in Auto Expose Time Mode | 0(default)。 | com.pi.pano.PilotSDK\#setISO |
| Sensitivity in Manual Expose Time Mode | 100、200、400、600、800(default)、1000、1600、3200、6400。 | com.pi.pano.PilotSDK\#setISO |
| White Balance | auto：Auto(default)；daylight；cloudy-daylight；incandescent；fluorescent。 | com.pi.pano.PilotSDK\#setWhiteBalance |

## Timelapse Video(tlv)

### Basic parameters

| **function** | **Value** | **Reference Api** |
|:-|:-|:-|
| Steady | false：Off；true：On(default)。 | com.pi.pano.PilotSDK\#useGyroscope |
| Resolution | \_5.7K：5760X2880(default)。 | com.pi.pano.PilotSDK\#startRecord |
| Encode | H.264(default)、H.265。 | com.pi.pano.PilotSDK\#startRecord |
| Frame Rate | 30(default)。 | com.pi.pano.PilotSDK\#startRecord |
| Steady Orientation | fix：Fixed(default)；follow：Follow camera orientation。 | - |

### Pro parameters

| **function** | **Value** | **Reference Api** |
|:-|:-|:-|
| Exposure Time Mode | 0：Auto(default)；1：Manual。 | - |
| Exposure Time | 0(default)、3200、2000、1000、500、250、120。 | com.pi.pano.PilotSDK\#setExposeTime |
| Exposure Compensation | -4、-3、-2、-1、0(default)、1、2、3、 4。 | com.pi.pano.PilotSDK\#setExposureCompensation |
| Sensitivity in Auto Expose Time Mode | 0(default)。 | com.pi.pano.PilotSDK\#setISO |
| Sensitivity in Manual Expose Time Mode | 100、200、400、600、800(default)、1000、1600、3200、6400。 | com.pi.pano.PilotSDK\#setISO |
| White Balance | auto：Auto(default)；daylight；cloudy-daylight；incandescent；fluorescent。 | com.pi.pano.PilotSDK\#setWhiteBalance |

## Street View Video(stv)

### Basic parameters

| **function** | **Value** | **Reference Api** |
|:-|:-|:-|
| Steady | false：Off(default)；true：On。 | com.pi.pano.PilotSDK\#useGyroscope |
| Resolution | \_5.7K：5760X2880(default)。 | com.pi.pano.PilotSDK\#startRecord |
| Encode | H.264(default)、H.265。 | com.pi.pano.PilotSDK\#startRecord |
| Frame Rate | \_5.7K：7(default)，4，3，2，1。 | com.pi.pano.PilotSDK\#startRecord |
| Steady Orientation | fix：Fixed(default)、follow：Follow camera orientation。 | - |

### Pro parameters

| **function** | **Value** | **Reference Api** |
|:-|:-|:-|
| Exposure Time Mode | 0：Auto(default)、1：Manual。 | - |
| Exposure Time | 0(default)、3200、2000、1000、500、250、120。 | com.pi.pano.PilotSDK\#setExposeTime |
| Exposure Compensation | -4、-3、-2、-1、0(default)、1、2、3、4。 | com.pi.pano.PilotSDK\#setExposureCompensation |
| Sensitivity in Auto Expose Time Mode | 0(default)。 | com.pi.pano.PilotSDK\#setISO |
| Sensitivity in Manual Expose Time Mode | 100、200、400、600、800(default)、1000、1600、3200、6400。 | com.pi.pano.PilotSDK\#setISO |
| White Balance | auto：Auto(default)；daylight；cloudy-daylight；incandescent；fluorescent。 | com.pi.pano.PilotSDK\#setWhiteBalance |
