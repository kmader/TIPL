# Serialization
As all objects must be ```Serializable``` to be used as part of ```RDD``` operations in Spark, it can be difficult to work with libraries which do not implement these featuers.

## Java Solutions
### Simple Classes
For simple classes, it is easiest to make a wrapper interface that extends Serializable. This means that even though ```UnserializableObject``` cannot be serialized we can pass in the following object without any issue

```{java}
public interface UnserializableWrapper extends Serializable {
    public UnserializableObject create(String parm1, String parm2);
}
```
The object can then be passed into an RDD or Map function using the following approach
```{java}
UnserializableWrapper usw = new UnserializableWrapper() {
    public UnserializableObject create(String parm1, String parm2) {
        return new UnserializableObject(parm1,parm2);
    }
}
```

## Scala Solution
### Simple Classes

For simple classes, it is easiest to take advantage of the fact that lambda functions are by definition Serializable. This means that even though ```UnserializableObject``` cannot be serialized we can pass in the following object without any issue
```{scala}
val serializableLambda = () => new UnserializableObject(parm1,parm2)
```

### Complex Classes
 For many classes (like images or matrices) their representations for processing and storage can be different, and the following approach enables the object to be used in either form without paying conversion costs until it is needed. Particularly in Spark where there might be many ```map``` or ```mapPartitions``` operations before a ```reduce``` or ```partitionBy``` forces the object to be serialized and send to another node. An approach which converted between every step would be very inefficient
 The scala solution we have come up with involves taking advantage of the ```Externalizable``` interface and the ```Either``` type. The following representation allows for the object to be stored as ```UnserializableObject``` and processed further and only converted to ```SerializableObject``` when it is serialized.

```{scala}
trait SparkSafeObject extends Externalizable {
    /**
    the storage for the data, it needs to be var since the Externalizer will have to modify it after instantiation
    **/
    var coreObject: Either[SerializableObject,UnserializableObject]
    /**
    these functions convert back and forth between the two types and are essential for this to work
    **/
    def serToUnser(so: SerializableObject): UnserializableObject
    def unserToSer(so: UnserializableObject): SerializableObject

    private def serialObject = coreObject match {
      case Left(so) => so
      case Right(uso) => unserToSer(uso)
    }

    private def unserialObject = coreObject match {
      case Left(so) => serToUnser(so)
      case Right(uso) => uso
    }
    /**
    The lazy val here ensures it is only called once (if needed), and the result is cached
    **/
    lazy val getSerializableObject = serialObject
    lazy val getUnserializableObject = unserialObject
    /**
     * custom serialization writes just the serialiableboject to the file
     * @param out the ObjectOutput to write everything to
     */
    @throws[IOException]("if the file doesn't exist")
    override def writeExternal(out: ObjectOutput): Unit = {
      out.writeObject(getSerializableObject)
    }

    /**
     * custom serialization for reading in these objects
     * @param in the input stream to read from
     */
    @throws[IOException]("if the file doesn't exist")
    @throws[ClassNotFoundException]("if the class cannot be found")
    override def readExternal(in: ObjectInput): Unit = {
      coreObject = Left(in.readObject.asInstanceOf[SerializableObject])
    }
}
```