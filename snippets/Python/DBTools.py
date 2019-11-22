import os
import re
import md5
import numpy as np
import socket
import time

# make this work on os_xx
# sudo install_name_tool -change libmysqlclient.18.dylib /usr/local/mysql/lib/libmysqlclient.18.dylib /Applications/Sage-4.7.2-OSX-64bit-10.6.app/Contents/Resources/sage/local/lib/python2.6/site-packages/MySQL_python-1.2.3-py2.6-macosx-10.7-x86_64.egg/_mysql.so
def rndSuf(nchars=6):
    return strcsv(md5.md5(str(time.time()) + "wow").hexdigest())[0:nchars]


seqNameCount = 0


def seqName(nchars=10):
    outStr = "%0" + str(nchars) + "d"
    globals()["seqNameCount"] += 1
    if globals()["seqNameCount"] >= 10 ** (nchars - 1):
        globals()["seqNameCount"] = 0
    return outStr % globals()["seqNameCount"]


# Master Project List
projects = {}
defPre = "/DISK63/DATA/SLS/PROJECTS/*"
defSuf = "/doc/csv"
# project['Abbreviated Name'] = ('Project Title',Path Prefix (Drive/Folder), Path Suffix (where csv files are),File Names Need, File names shouldn't have, Is a Local Tomography Measurement,Which Database File)
projects["UJX"] = ("ULTRAJAX", defPre, defSuf, "", "", False, "Lacuna")
projects["JBM"] = ("ULTRAJAX/JBMR", defPre, defSuf, "", "", True, "Lacuna")
projects["UST"] = ("ULTRAJAX/SOST", defPre, defSuf, "", "", True, "Lacuna")
# projects['UST2']=('ULTRAJAX/2DSOST',defPre,defSuf,'','',True,'Lacuna')
projects["VAL"] = ("ULTRAJAX/VALIDATE", defPre, defSuf, "", "SPH", True, "Validate")
projects["MISC"] = ("ULTRAJAX/OTHER", defPre, defSuf, "", "", True, "Other")
projects["BIN"] = (
    "BINTEST",
    "/DISK72/DATA/SLS/PROJECTS/*",
    defSuf,
    "",
    "",
    False,
    "Lacuna",
)

projects["PR1"] = (
    "ULTRAJAX_PR",
    "/DISK72/DATA/SLS/PROJECTS/ULTRAJAX_B1",
    defSuf,
    "PR",
    "F2",
    False,
    "Lacuna",
)
projects["PR2"] = (
    "ULTRAJAX_PR",
    "/DISK83/DATA/SLS/PROJECTS/ULTRAJAX_B2",
    defSuf,
    "PR",
    "F2",
    False,
    "Lacuna",
)
projects["UF3"] = (
    "ULTRAJAX_F2",
    "/DISK83/DATA/SLS/PROJECTS/ULTRAJAX_B100530",
    defSuf,
    "",
    "",
    False,
    "Lacuna",
)
projects["UF2"] = (
    "ULTRAJAX_F2",
    "/DISK83/DATA/SLS/PROJECTS/UJAX_B1009A",
    defSuf,
    "",
    "",
    False,
    "Lacuna",
)

# projects['DIG']=('DIGFA','/DISK73/DATA/SLS/PROJECTS/DIGFA/KEVIN',defSuf,'','',True,'Lacuna')
# projects['ADIG']=('DIGFA/ALINA','/DISK73/DATA/SLS/PROJECTS/DIGFA/LEVCHUK',defSuf,'','',True,'Lacuna')
# projects['ASTR']=('DIGFA/STRAIN','/DISK73/DATA/SLS/PROJECTS/DIGFA/STRAIN',defSuf,'','',True,'Lacuna')
# Philipp Aging Study
projects["REPK"] = (
    "REPRO/KEVIN",
    "/DISK83/DATA/SLS/PROJECTS/REPRO/KEVIN",
    defSuf,
    "",
    "",
    True,
    "Lacuna",
)
projects["REPF"] = (
    "REPRO/FELIX",
    "/DISK83/DATA/SLS/PROJECTS/REPRO/FELIX",
    defSuf,
    "",
    "",
    True,
    "Lacuna",
)
projects["REPF2"] = (
    "REPRO/FELIX2",
    "/DISK83/DATA/SLS/PROJECTS/REPRO/FELIX",
    defSuf,
    "",
    "",
    True,
    "Lacuna",
)
projects["REPB6"] = (
    "REPRO/B6",
    "/DISK83/DATA/SLS/PROJECTS/REPRO/B6",
    defSuf,
    "",
    "",
    True,
    "Lacuna",
)
projects["REPC3"] = (
    "REPRO/C3",
    "/DISK83/DATA/SLS/PROJECTS/REPRO/C3",
    defSuf,
    "",
    "",
    True,
    "Lacuna",
)
projects["DEVEL"] = (
    "DEVELOP",
    "/DISK83/DATA/SLS/PROJECTS/DEVELOP",
    defSuf,
    "",
    "",
    True,
    "Lacuna",
)

# Floor project
# projects['3XL']=('SINGLE','/DISK82/DATA/UCT/PROJECTS/FML/SINGLE_SLS',defSuf,'','',False,'Lacuna')
# projects['OVX']=('LOADING','/DISK82/DATA/UCT/PROJECTS/FML/OVX_SLS',defSuf,'','',False,'Lacuna')

# Temp Junk Projects
floatlist = lambda d: map(float, list(d))
nprange = lambda x: (max(x) - min(x))
persistentColoringMap = {}
persistentColoringCount = 0


def resetMaps():
    globals()["persistentColoringMap"] = {}
    globals()["persistentStylingMap"] = {}
    globals()["colorList"] = "rgbcmyk"
    globals()["styleList"] = ".^x*osh+"


colorList = "rgbcmyk"


def consistentColoringFunction(cName):
    colorList = globals()["colorList"]
    if not globals()["persistentColoringMap"].has_key(cName):
        globals()["persistentColoringMap"][cName] = colorList[
            globals()["persistentColoringCount"] % len(colorList)
        ]
        globals()["persistentColoringCount"] += 1
    return globals()["persistentColoringMap"][cName]


persistentStylingMap = {}
persistentStylingCount = 0
styleList = ".^x*osh+"


def consistentStylingFunction(cName):
    styleList = globals()["styleList"]
    if not globals()["persistentStylingMap"].has_key(cName):
        globals()["persistentStylingMap"][cName] = styleList[
            globals()["persistentStylingCount"] % len(styleList)
        ]
        globals()["persistentStylingCount"] += 1
    return globals()["persistentStylingMap"][cName]


## Curve Fitting Models for SQLHistPlot
logParabolaFit = (
    lambda xdat, ydat: (
        nprange(log10(ydat)) / nprange(log10(xdat)),
        mean(log10(xdat)),
        max(log10(ydat)),
    ),
    lambda xdat, p: np.power(10, p[2] - p[0] * (log10(xdat) - p[1]) ** 2),
)


def spow(x, y):

    try:
        return math.pow(x, y)
    except:
        return 0


# Database Math Functions
def lacdb_AddMathFcn(fcnName, fcnArgs, fcn):
    ## Create 'Safe' math functions in SQLite
    def safeFcn(*args):
        try:
            return apply(fcn, args)
        except:
            "LDB_AMF - Problem in : " + str(fcnName) + " invalid input : " + str(args)
        return 0

    try:
        con.create_function(fcnName, fcnArgs, safeFcn)
    except:
        con.createscalarfunction(fcnName, safeFcn)


def is_numeric(lit):
    "Return value of numeric literal string or ValueError exception"

    try:
        f = float(lit)
        return True
    except ValueError:
        return False


class lacDB_UNICNT:
    def __init__(self):
        self.items = {}

    def step(self, value):
        if value is None:
            return 0
        self.items[value] = 1

    def finalize(self):
        oVal = len(self.items.keys())
        if oVal is None:
            oVal = 0
        return oVal

    @classmethod
    def factory(cls):
        return cls(), cls.step, cls.finalize


nullVals = lambda cVar: cur.execute(
    "SELECT SAMPLE_AIM_NUMBER,COUNT(*) From Lacuna WHERE "
    + cVar
    + " is not IFNULL("
    + cVar
    + ",-1.1) GROUP BY SAMPLE_AIM_NUMBER"
).fetchall()


def Grid2D(varX="POS_X", varY="POS_Y", stepX=5.0 / 100, stepY=None):
    ## 2D Grid for Group By Function in SQL
    if stepY is None:
        stepY = stepX
    return (
        "(FLOOR(("
        + str(varX)
        + ")/"
        + str(stepX)
        + ") || FLOOR(("
        + str(varY)
        + ")/"
        + str(stepY)
        + "))"
    )


def Grid3D(
    varX="POS_X", varY="POS_Y", varZ="POS_Z", stepX=5.0 / 100, stepY=None, stepZ=None
):
    ## 3D Grid for Group By Function
    if stepY is None:
        stepY = stepX
    if stepZ is None:
        stepZ = stepY
    return (
        "(FLOOR(("
        + str(varX)
        + ")/"
        + str(stepX)
        + ") || FLOOR(("
        + str(varY)
        + ")/"
        + str(stepY)
        + ") || FLOOR(("
        + str(varZ)
        + ")/"
        + str(stepZ)
        + "))"
    )


def GridVar(varX="POS_X", stepX=5.0 / 100):
    ## For Later Plots
    return "(" + str(stepX) + "*FLOOR((" + str(varX) + ")/" + str(stepX) + "))"


class CaseFreeDict:
    """Dictionary, that has case-insensitive keys.
    
    Keys are retained in their original form
    when queried with .keys() or .items().

    Implementation: An internal dictionary maps lowercase
    keys to (key,value) pairs. All key lookups are done
    against the lowercase keys, but all methods that expose
    keys to the user retrieve the original keys."""

    def __init__(self, dict=None):
        """Create an empty dictionary, or update from 'dict'."""
        self._dict = {}
        if dict:
            self.update(dict)

    def __getitem__(self, key):
        """Retrieve the value associated with 'key' (in any case)."""
        k = key.lower()
        return self._dict[k][1]

    def __setitem__(self, key, value):
        """Associate 'value' with 'key'. If 'key' already exists, but
        in different case, it will be replaced."""
        k = key.lower()
        self._dict[k] = (key, value)

    def has_key(self, key):
        """Case insensitive test wether 'key' exists."""
        k = key.lower()
        return self._dict.has_key(k)

    def keys(self):
        """List of keys in their original case."""
        return [v[0] for v in self._dict.values()]

    def values(self):
        """List of values."""
        return [v[1] for v in self._dict.values()]

    def items(self):
        """List of (key,value) pairs."""
        return self._dict.values()

    def get(self, key, default=None):
        """Retrieve value associated with 'key' or return default value
        if 'key' doesn't exist."""
        try:
            return self[key]
        except KeyError:
            return default

    def setdefault(self, key, default):
        """If 'key' doesn't exists, associate it with the 'default' value.
        Return value associated with 'key'."""
        if not self.has_key(key):
            self[key] = default
        return self[key]

    def update(self, dict):
        """Copy (key,value) pairs from 'dict'."""
        for k, v in dict.items():
            self[k] = v

    def __repr__(self):
        """String representation of the dictionary."""
        items = ", ".join([("%r: %r" % (k, v)) for k, v in self.items()])
        return "{%s}" % items

    def __str__(self):
        """String representation of the dictionary."""
        return repr(self)


class mysqlCurWrapper:
    """ A wrapper for the my sql function to remove case sensitivity (make everything uppercase) and return the cursor when queries are executed -> cur.execute(...).fetchall() now works """

    def __init__(self, rCon, regenCall=lambda x: None):
        self.__connection__ = rCon
        self.__cursor__ = rCon.cursor()
        self.regenCall = regenCall
        self.verbose = False

    def _fixquery(self, qryText, qryData):
        qryText = "".join(qryText.upper().split("BEGIN;"))
        qryText = "".join(qryText.upper().split("BEGIN ;"))
        qryText = "".join(qryText.upper().split("BEGIN"))
        qryText = "".join(qryText.upper().split("COMMIT;"))
        qryText = "".join(qryText.upper().split("COMMIT ;"))
        qryText = "".join(qryText.upper().split("COMMIT"))
        qrySplit = qryText.split("?")
        oQry = "%s".join(qrySplit)
        # print oQry
        return oQry
        qrySplit.reverse()
        qryOut = qrySplit.pop()
        qrySplit.reverse()
        for (val, qryPostfix) in zip(qryData, qrySplit):
            tv = type(val)
            if True:
                qryOut += "%s"  # very boring options
            elif tv is type(""):
                qryOut += "%s"
            elif tv is type(1):
                qryOut += "%s"
            elif tv is int:
                qryOut += "%d"
            elif tv is type(1.0):
                qryOut += "%f"
            elif tv is float:
                qryOut += "%f"
            else:
                qryOut += "%s"
            qryOut += qryPostfix

        # print qryOut
        return qryOut

    def prepStatement(self, inStatement, inVals):
        return inStatement % self._get_db().literal(inVals)

    def execute(self, *args, **keywords):
        if self.verbose:
            print ("exec:", args, keywords)
        nargs = list(args)
        nargs[0] = nargs[0].upper()
        if len(nargs) > 1:
            nargs[0] = self._fixquery(nargs[0], nargs[1])
        self.__cursor__.execute(*tuple(nargs), **keywords)
        return self

    def executemany(self, *args, **keywords):
        if self.verbose:
            print ("execmany:", args[0], keywords)
        nargs = list(args)
        nargs[0] = nargs[0].upper()
        if len(nargs) > 1:
            nargs[0] = self._fixquery(nargs[0], nargs[1][0])
        # print nargs
        self.__cursor__.executemany(*nargs, **keywords)
        return self

    def begin(self):
        return self.__connection__.begin()

    def commit(self):
        return self.__connection__.commit()

    def refresh(self):
        nCur = self.regenCall(0)
        self.__connection__ = nCur.__connection__
        self.__cursor__ = nCur.__cursor__

    def __getattr__(self, what):
        # print (what,type(what),'is missing checking mysql')
        try:
            return getattr(self.__cursor__, what)
        except:
            return getattr(self.__connection__, what)


def StartDatabase(
    dbName="Lacuna",
    debugMode=False,
    apsw=True,
    mysql=True,
    dorw=True,
    doad=None,
    mysqlHost="tomquant.sql.psi.ch",
):
    hostComputer = socket.gethostname().upper()
    if debugMode:
        dbName = dbName + "-Debug"
    regenFunc = lambda x: StartDatabase(
        dbName,
        debugMode=debugMode,
        apsw=apsw,
        mysql=mysql,
        dorw=dorw,
        doad=doad,
        mysqlHost=mysqlHost,
    )
    if mysql:
        if dorw:
            globals()["homeDb"] = (mysqlHost, "tomquant_rw", "8y0jz0", "tomquant")
        else:
            globals()["homeDb"] = (mysqlHost, "tomquant_ro", "OL5iGe", "tomquant")
        if doad is not None:
            globals()["homeDb"] = (mysqlHost, "tomquant_ad", doad, "tomquant")
        print "Sage is Running on : " + hostComputer + " using database - " + str(
            globals()["homeDb"]
        )
        import MySQLdb as mdb

        globals()["con"] = mdb.connect(*globals()["homeDb"])
        globals()["cur"] = mysqlCurWrapper(globals()["con"], regenCall=regenFunc)
        lacTemp = {}
        return globals()["cur"]
    else:
        cPath = os.path.realpath(os.path.realpath(sys.argv[0]))
        cPath = os.path.expandvars("$HOME/nt/u/OpenVMS/JAX/Sage/FTPTools.py")
        # DBCompiled should be in the same directory
        dbCScript = "/".join(cPath.split("/")[:-1] + ["DBCompiled.pyx"])

        print "DBC=" + dbCScript

        sage.all.attach(dbCScript)
        if hostComputer.find("STERNUM") >= 0:  # On Sternum
            globals()["homeDb"] = os.path.expandvars(
                "/project/sageuser/" + dbName + ".db"
            )
        if hostComputer.find("X02DA") >= 0:  # On Beamline
            globals()["homeDb"] = os.path.expandvars("$HOME/Data10/" + dbName + ".db")
        elif hostComputer.find("PC7819") >= 0:  # On PC7819
            globals()["homeDb"] = os.path.expandvars("/home/scratch/" + dbName + ".db")
        else:  # On Kevins Laptop
            globals()["homeDb"] = os.path.expandvars("$HOME/" + dbName + ".db")
        print "Sage is Running on : " + hostComputer + " using database - " + globals()[
            "homeDb"
        ]
        if apsw:
            import apsw

            globals()["con"] = apsw.Connection(
                globals()["homeDb"],
                flags=apsw.SQLITE_OPEN_READWRITE | apsw.SQLITE_OPEN_CREATE,
            )
            globals()["cur"] = globals()["con"].cursor()
            print globals()["cur"].execute("PRAGMA cache_size=-400000").fetchall()
            print globals()["cur"].execute("PRAGMA temp_store=2").fetchall()
            # print globals()['cur'].execute('PRAGMA journal_mode=WAL').fetchall()
            print globals()["cur"].execute("PRAGMA journal_mode=DELETE").fetchall()
            lacTemp = {}
            SetupAPSWDatabase()
            globals()["cur"] = globals()["con"].cursor()
            return globals()["cur"]
        else:
            ## Initialize Database Code, Functions
            try:
                from sage.databases.database import SQLDatabase
            except:
                from sage.databases.sql_db import SQLDatabase
            if sage0_version().find("4.6.1") >= 0:
                globals()["lacDB"] = SQLDatabase(globals()["homeDb"])
            else:
                globals()["lacDB"] = SQLDatabase(globals()["homeDb"], read_only=False)
            globals()["lacTemp"] = globals()["lacDB"].get_skeleton()["Lacuna"].items()
            SetupDatabase(lacDB)
            return globals()["cur"]


def SetupAPSWDatabase():
    # Database Code
    # create cursors and connections

    lacdb_AddMathFcn("sqrt", 1, math.sqrt)
    lacdb_AddMathFcn("is_numeric", 1, is_numeric)
    lacdb_AddMathFcn("sq", 1, lambda x: x * x)
    lacdb_AddMathFcn("pow", 2, lambda x, y: spow(x, y))
    lacdb_AddMathFcn("acos", 1, lambda x: 180 / pi * math.acos(x))
    lacdb_AddMathFcn("asin", 1, lambda x: 180 / pi * math.asin(x))
    lacdb_AddMathFcn("atan", 1, lambda x: 180 / pi * math.atan(x))
    lacdb_AddMathFcn("atan2", 2, lambda x, y: 180 / pi * math.atan2(x, y))
    lacdb_AddMathFcn("cos", 1, lambda x: math.cos(pi / 180.0 * x))
    lacdb_AddMathFcn("log10", 1, lambda x: math.log10(x))
    lacdb_AddMathFcn("ln", 1, lambda x: math.log(x))
    lacdb_AddMathFcn("exp", 1, lambda x: math.exp(x))
    lacdb_AddMathFcn("sin", 1, lambda x: math.sin(pi / 180.0 * x))
    lacdb_AddMathFcn("tan", 1, lambda x: math.tan(pi / 180.0 * x))
    lacdb_AddMathFcn("floor", 1, lambda x: floor(x))
    lacdb_AddMathFcn("linspace", 4, clacdb_LINSPACE)
    lacdb_AddMathFcn("flinspace", 4, clacdb_FLINSPACE)
    lacdb_AddMathFcn("fixang", 1, c_fixang)

    APSW_createscalar("zame", 2, c_zame2)
    APSW_createscalar("zame", 3, c_zame3)
    APSW_createscalar("split", 3, clacdb_SPLIT)
    APSW_createscalar("fsplit", 3, clacdb_FSPLIT)
    APSW_createscalar("isplit", 3, clacdb_ISPLIT)
    APSW_createscalar("RDIST", 6, clacdb_rdist)

    APSW_createaggregate("med", 1, clacDB_MED)

    APSW_createaggregate("var", 1, clacDB_VAR)
    APSW_createaggregate("unicnt", 1, lacDB_UNICNT)
    APSW_createaggregate("std", 1, clacDB_STD)
    APSW_createaggregate("wavg", 2, clacDB_WAVG)
    APSW_createaggregate("wvar", 2, clacDB_WVAR)
    APSW_createaggregate("wstd", 2, clacDB_WSTD)
    APSW_createaggregate("linfit", 2, clacDB_LINFIT)
    APSW_createaggregate("wlinfit", 3, clacDB_WLINFIT)
    APSW_createaggregate("corr", 2, clacDB_CORR)
    APSW_createaggregate("ttest", 2, clacDB_TTEST)
    APSW_createaggregate("texture", 3, clacDB_TEXT)
    APSW_createaggregate("wtexture", 4, clacDB_TEXT)


def APSW_createscalar(cName, cCount, cFunc):
    con.createscalarfunction(cName, cFunc)


def APSW_createaggregate(cName, cCount, cClass):
    con.createaggregatefunction(cName, cClass.factory)


def SetupDatabase(lacDB):
    # Database Code
    # create cursors and connections
    globals()["cur"] = lacDB.get_cursor()
    globals()["con"] = lacDB.get_connection()
    lacdb_AddMathFcn("sqrt", 1, math.sqrt)
    lacdb_AddMathFcn("is_numeric", 1, is_numeric)
    lacdb_AddMathFcn("sq", 1, lambda x: x * x)
    lacdb_AddMathFcn("pow", 2, lambda x, y: spow(x, y))
    lacdb_AddMathFcn("acos", 1, lambda x: 180 / pi * math.acos(x))
    lacdb_AddMathFcn("asin", 1, lambda x: 180 / pi * math.asin(x))
    lacdb_AddMathFcn("atan", 1, lambda x: 180 / pi * math.atan(x))
    lacdb_AddMathFcn("atan2", 2, lambda x, y: 180 / pi * math.atan2(x, y))
    lacdb_AddMathFcn("cos", 1, lambda x: math.cos(pi / 180.0 * x))
    lacdb_AddMathFcn("log10", 1, lambda x: math.log10(x))
    lacdb_AddMathFcn("ln", 1, lambda x: math.log(x))
    lacdb_AddMathFcn("exp", 1, lambda x: math.exp(x))
    lacdb_AddMathFcn("sin", 1, lambda x: math.sin(pi / 180.0 * x))
    lacdb_AddMathFcn("tan", 1, lambda x: math.tan(pi / 180.0 * x))
    lacdb_AddMathFcn("floor", 1, lambda x: floor(x))
    lacdb_AddMathFcn("linspace", 4, clacdb_LINSPACE)
    lacdb_AddMathFcn("flinspace", 4, clacdb_FLINSPACE)
    lacdb_AddMathFcn("fixang", 1, c_fixang)

    con.create_function("zame", 2, c_zame2)
    con.create_function("zame", 3, c_zame3)
    con.create_function("split", 3, clacdb_SPLIT)
    con.create_function("fsplit", 3, clacdb_FSPLIT)
    con.create_function("isplit", 3, clacdb_ISPLIT)
    con.create_function("RDIST", 6, clacdb_rdist)
    con.create_aggregate("med", 1, clacDB_MED)

    con.create_aggregate("var", 1, clacDB_VAR)
    con.create_aggregate("unicnt", 1, lacDB_UNICNT)
    con.create_aggregate("std", 1, clacDB_STD)
    con.create_aggregate("wavg", 2, clacDB_WAVG)
    con.create_aggregate("wvar", 2, clacDB_WVAR)
    con.create_aggregate("wstd", 2, clacDB_WSTD)
    con.create_aggregate("linfit", 2, clacDB_LINFIT)
    con.create_aggregate("wlinfit", 3, clacDB_WLINFIT)
    con.create_aggregate("corr", 2, clacDB_CORR)
    con.create_aggregate("ttest", 2, clacDB_TTEST)
    con.create_aggregate("texture", 3, clacDB_TEXT)
    con.create_aggregate("wtexture", 4, clacDB_TEXT)


# See if there are any objects from this sample in the database
def getSampleCount(cur, sampleNum, tableName="Lacuna"):
    cQry = cur.execute(
        "SELECT COUNT(*) FROM " + tableName + " WHERE SAMPLE_AIM_NUMBER = ?",
        (int(sampleNum),),
    ).fetchone()
    return cQry[0]


# Loopkup the sample number based on path
def getSampleNumFromPath(cur, dataPath, projNum):
    cQry = cur.execute(
        "SELECT SAMPLE_AIM_NUMBER,SAMPLE_AIM_NAME FROM SAMPLE WHERE PROJECT_NUMBER=? AND DATA_PATH LIKE ?",
        (int(projNum), dataPath),
    ).fetchall()
    print cQry
    if len(cQry) > 0:
        is_match = cQry[0][0]
    else:
        is_match = -1
    if is_match < 0:
        print dataPath + " could not be found in project " + str(
            projNum
        ) + " not enough information to create it!"
    return is_match


# Sample Metrics Database Interface
def toMetricName(inName):
    return inName.upper().strip()


def getMetrics(cur, metricName):
    cQry = cur.execute(
        "SELECT SAMPLE_AIM_NUMBER,VALUE FROM SAMPLEMETRICS WHERE NAME = ?",
        (toMetricName(metricName),),
    ).fetchall()
    return dict(cQry)


def getSampleMetrics(cur, sampleNum):
    cQry = cur.execute(
        "SELECT NAME,VALUE FROM SAMPLEMETRICS WHERE SAMPLE_AIM_NUMBER = ?", (sampleNum,)
    ).fetchall()
    return dict(cQry)


def getSampleMetricsString(cur, sampleNum):
    cQry = cur.execute(
        "SELECT NAME,STRINGVALUE FROM SAMPLEMETRICS WHERE SAMPLE_AIM_NUMBER = ?",
        (sampleNum,),
    ).fetchall()
    return dict(cQry)


def addSampleMetrics(cur, sampleNum, nameVal, valVal=-1, strVal=""):
    try:
        cur.execute(
            "INSERT INTO SAMPLEMETRICS (SAMPLE_AIM_NUMBER,NAME,VALUE,STRINGVALUE) VALUES (?,?,?,?)",
            (sampleNum, toMetricName(nameVal), valVal, strVal),
        )
    except:
        cur.execute(
            "UPDATE SAMPLEMETRICS SET VALUE=?,STRINGVALUE=? WHERE SAMPLE_AIM_NUMBER=? AND NAME=?",
            (valVal, strVal, sampleNum, toMetricName(nameVal)),
        )


# Loopkup the current sample number and if it does not exist, make it
def getSampleNum(cur, sampleName, projNum, tries=0, doInsert=False, dataPath=None):
    if dataPath is None:
        dataPath = sampleName
    if sampleName is None:
        return getSampleNumFromPath(cur, dataPath, projNum)
    print ("le sample", sampleName)
    if tries > 2:
        print "getSampleNum has failed, please check the integrity of the database"
        return -1
    wArgs = ["SAMPLE_AIM_NAME LIKE ?"]
    if type(projNum) is type(""):
        projNum = getProjNum(cur, projNum, doInsert=doInsert)
    if projNum is not None:
        wArgs += ["PROJECT_NUMBER=%i" % int(projNum)]
    cQry = cur.execute(
        "SELECT SAMPLE_AIM_NUMBER,SAMPLE_AIM_NAME FROM SAMPLE WHERE "
        + " AND ".join(wArgs),
        (sampleName),
    ).fetchall()
    print cQry
    if len(cQry) > 0:
        is_match = cQry[0][0]
    else:
        is_match = 0
    if is_match < 1:
        if doInsert:
            try:
                print "Creating Sample : " + sampleName + " // " + dataPath
                cur.execute(
                    "INSERT INTO SAMPLE (PROJECT_NUMBER,SAMPLE_AIM_NAME,DATA_PATH) VALUES (?,?,?)",
                    (projNum, sampleName, dataPath),
                )
                cur.commit()
            except:
                print "Creating Sample " + sampleName + " Failed!"
                cur.rollback()
            return getSampleNum(
                cur,
                sampleName,
                projNum,
                tries + 1,
                doInsert=doInsert,
                dataPath=dataPath,
            )
        else:
            return -1

    else:
        return is_match


# Loopkup the current project number and if it does not exist, make it


def getProjNum(cur, projName, tries=0, doInsert=False):
    if tries > 2:
        print "getProjNum has failed, please check the integrity of the database"
        return -1
    cQry = cur.execute(
        "SELECT PROJECT_NUMBER,PROJECT_NAME FROM PROJECT WHERE PROJECT_NAME LIKE ?",
        (projName,),
    ).fetchall()
    print cQry
    if len(cQry) > 0:
        is_match = cQry[0][0]
    else:
        is_match = 0
    if is_match < 1:
        if doInsert:
            try:
                cur.execute(
                    "INSERT INTO PROJECT (PROJECT_NAME) VALUES (?)", (projName,)
                )
                cur.commit()
            except:
                print "Creating Project Failed!"
                cur.rollback()
            return getProjNum(cur, projName, tries + 1, doInsert=doInsert)
        else:
            return -1
    else:
        return is_match


crossprod = lambda va, vb: (
    "((" + va + "_Y)*(" + vb + "_Z)-(" + va + "_Z)*(" + vb + "_Y))",
    "((" + va + "_Z)*(" + vb + "_X)-(" + va + "_X)*(" + vb + "_Z))",
    "((" + va + "_X)*(" + vb + "_Y)-(" + va + "_Y)*(" + vb + "_X))",
)

saimText = "(SELECT SA.SAMPLE_AIM_NAME FROM SAMPLE SA WHERE SA.SAMPLE_AIM_NUMBER=SAMPLE_AIM_NUMBER AND SA.PROJECT_NUMBER=PROJECT_NUMBER)"
paimText = (
    "(SELECT PA.PROJECT_NAME FROM PROJECT PA WHERE PA.PROJECT_NUMBER=PROJECT_NUMBER)"
)

strSecCanAng = "MIN(ACOS(CANAL_GRAD_X*PCA2_X+CANAL_GRAD_Y*PCA2_Y+CANAL_GRAD_Z*PCA2_Z),ACOS(-CANAL_GRAD_X*PCA2_X-CANAL_GRAD_Y*PCA2_Y-CANAL_GRAD_Z*PCA2_Z))"
strSecMaskAng = "MIN(ACOS(MASK_GRAD_X*PCA2_X+MASK_GRAD_Y*PCA2_Y+MASK_GRAD_Z*PCA2_Z),ACOS(-MASK_GRAD_X*PCA2_X-MASK_GRAD_Y*PCA2_Y-MASK_GRAD_Z*PCA2_Z))"
fullFieldNames = {}
fullFieldNames["SAMPLE_AIM_NAME"] = "Sample Name"
fullFieldNames["POS_X*1000"] = "X-Position ($\mu m$)"
fullFieldNames["POS_Y*1000"] = "Y-Position ($\mu m$)"
fullFieldNames["POS_Z*1000"] = "Z-Position ($\mu m$)"

fullFieldNames["CANAL_DISTANCE_MEAN*1000"] = "Canal Distance ($\mu m$)"
fullFieldNames["MASK_DISTANCE_MEAN*1000"] = "Distance to Bone Surface ($\mu m$)"
fullFieldNames["CANAL_ANGLE"] = "Orientation to Nearest Canal"
fullFieldNames[strSecCanAng] = "Seconday Canal Angle"
fullFieldNames[strSecMaskAng] = "Seconday Mask Angle"
fullFieldNames["MASK_ANGLE"] = "Orientation to Bone Surface"
fullFieldNames["(MASK_RADIUS_MAX-POS_RADIUS)*1000"] = "Perium Distance ($\mu m$)"
fullFieldNames["(POS_RADIUS-MASK_RADIUS_MIN)*1000"] = "Marrow Distance ($\mu m$)"
fullFieldNames["OBJ_RADIUS*1000"] = "Radius ($\mu m$)"
fullFieldNames["VOLUME"] = "Volume ($mm^3$)"
fullFieldNames["VOLUME*1000*1000*1000"] = "Volume ($\mu m^3$)"
fullFieldNames["PCA1_PHI"] = "Angle from XY Plane"
fullFieldNames["PCA2_THETA"] = "Secondary Orientation"
fullFieldNames["POW(VOLUME*1000*1000*1000,0.666667)"] = "$\ell$"
fullFieldNames["POW(VOLUME,0.666667)"] = "$\ell$"
fullFieldNames["PROJ_PCA1*1000"] = "Length ($\mu m$)"
fullFieldNames["PROJ_PCA2*1000"] = "Width ($\mu m$)"
fullFieldNames["PROJ_PCA3*1000"] = "Height ($\mu m$)"
fullFieldNames["2*SQRT(5)*PCA1_S*1000"] = "Length ($\mu m$)"
fullFieldNames["2*SQRT(5)*PCA2_S*1000"] = "Width ($\mu m$)"
fullFieldNames["2*SQRT(5)*PCA3_S*1000"] = "Height ($\mu m$)"
fullFieldNames["(VOLUME-VOLUME_LAYER)/VOLUME*100"] = "Roughness (\%)"
fullFieldNames[
    "4*3.14/3.0*OBJ_RADIUS*OBJ_RADIUS*OBJ_RADIUS/VOLUME*100"
] = "Sphericity (\%)"
fullFieldNames["(PCA1_S-PCA3_S)/PCA1_S"] = "Anisotropy Factor"
fullFieldNames["(PCA1_S-PCA3_S)/PCA1_S"] = "Anisotropy Factor (%)"
fullFieldNames["PROJ_PCA1/(PROJ_PCA2+PROJ_PCA3)*2"] = "Anisotropy"
fullFieldNames["PROJ_PCA2/PROJ_PCA3"] = "Disc Anisotropy (A23)"
fullFieldNames["2*(PCA2_S-PCA3_S)/(PCA1_S-PCA3_S)-1"] = "Oblateness"
fullFieldNames["PROJ_PCA3/PROJ_PCA1"] = "Anisotropy (A31)"
fullFieldNames["PROJ_PCA1/OBJ_RADIUS"] = "Anisotropy from Radius"
fullFieldNames["DENSITY_VOLUME*1000*1000*1000"] = "Lc.Unit Volume ($\mu m^3$)"
fullFieldNames["DENSITY"] = "Density (1/$mm^3$)"
fullFieldNames["DENSITY/1000"] = "Density (kLac/$mm^3$)"
fullFieldNames["SUM(VOLUME)/SUM(DENSITY_VOLUME)*100"] = "Volume Fraction ($\%$)"
fullFieldNames["THICKNESS*1000"] = "Thickness ($\mu m$)"
fullFieldNames["1/AVG(DENSITY_VOLUME)"] = "Density (1/$mm^3$)"
fullFieldNames["1/AVG(DENSITY_VOLUME)/1000"] = "Density (k/$mm^3$)"
fullFieldNames["POS_RADIUS*1000"] = "Radial Distance ($\mu m$)"
fullFieldNames["MASK_THETA"] = "Radial Angle ($\theta$)"
fullFieldNames["POS_DISTANCE*1000"] = "Center Distance ($\mu m$)"
fullFieldNames["ABS(PCA1_Z)*100"] = "Vertical Orientation (\%)"
fullFieldNames["VAR[PCA1_Z]"] = "Vertical Orientation Wobble"
fullFieldNames["VAR[PCA1_Z]*100"] = "Vertical Orientation Variation (\%)"
fullFieldNames["SHELL_ABSORPTION*1000"] = "Calcification Density (a.u.)"
fullFieldNames["SHELL_ABSORPTION_STD*1000"] = "Calcification Density Variation (a.u.)"
fullFieldNames["VOLUME/VOLUME_BOX*100"] = "Boxiness (\%)"
fullFieldNames["NEAREST_NEIGHBOR_NEIGHBORS"] = "Neighbors"
fullFieldNames["NEAREST_NEIGHBOR_DISTANCE*1000"] = "Nearest Neighbor Distance ($\mu$m)"
fullFieldNames[
    "NEAREST_NEIGHBOR_DISTANCE/POW(DENSITY_VOLUME/6.28,0.33333)"
] = "Self-Avoiding Coefficient"
fullFieldNames[
    "AVG(NEAREST_NEIGHBOR_DISTANCE)/AVG(POW(DENSITY_VOLUME,0.33333))"
] = "Self-Avoiding Coefficient"

fullFieldNames["TEXTURE(PCA1_X,PCA1_Y,PCA1_Z)"] = "Primary Lacuna Alignment ($\%$)"
fullFieldNames[
    "WTEXTURE(PCA1_X,PCA1_Y,PCA1_Z,PROJ_PCA1/PROJ_PCA2>1.5)"
] = "Primary Lacuna Alignment Corrected($\%$)"
fullFieldNames["TEXTURE(PCA2_X,PCA2_Y,PCA2_Z)"] = "Secondary Lacuna Alignment ($\%$)"
fullFieldNames[
    "WTEXTURE(PCA2_X,PCA2_Y,PCA2_Z,PROJ_PCA2/PROJ_PCA3>1.5)"
] = "Secondary Lacuna Alignment Corrected ($\%$)"
fullFieldNames[
    "1/(36*3.14)*DENSITY_VOLUME_SHELL*DENSITY_VOLUME_SHELL*DENSITY_VOLUME_SHELL/(DENSITY_VOLUME*DENSITY_VOLUME)-1"
] = "Voronoi Sphericity (a.u.)"
# Lacuna Displacement Paramters
strLacDisp = "DISPLACEMENT_MEAN*1000"
strLacNDisp = "100*DISPLACEMENT_MEAN/(2*POW(DENSITY_VOLUME,.333))"
strDispLac = "100*(DISPLACEMENT_MEAN>VOX_SIZE*2.2)"
strNDispLac = "100*(" + strLacNDisp + ">5)"

fullFieldNames[strLacDisp] = "Lacuna Displacement ($\mu m$)"
fullFieldNames[strLacNDisp] = "Normalized Lacuna Displacement ($\%$)"
fullFieldNames[strDispLac] = "Displaced Lacuna ($\%$)"
fullFieldNames[strNDispLac] = "Normalized Displaced Lacuna ($\%$)"

strLacACanAlign = "TEXTURE(" + ",".join(crossprod("PCA1", "CANAL_GRAD")) + ")"
strLacBCanAlign = "TEXTURE(" + ",".join(crossprod("PCA2", "CANAL_GRAD")) + ")"
strLacACanAlignW = (
    "WTEXTURE("
    + ",".join(crossprod("PCA1", "CANAL_GRAD"))
    + ",PROJ_PCA1/PROJ_PCA2>1.5)"
)
strLacBCanAlignW = (
    "WTEXTURE("
    + ",".join(crossprod("PCA2", "CANAL_GRAD"))
    + ",PROJ_PCA2/PROJ_PCA3>1.5)"
)

fullFieldNames[strLacACanAlign] = "Primary Lacuna Canal Alignment ($\%$)"
fullFieldNames[strLacBCanAlign] = "Secondary Lacuna Canal Alignment ($\%$)"
fullFieldNames[strLacACanAlignW] = "Primary Lacuna Canal Alignment ($\%$)"
fullFieldNames[strLacBCanAlignW] = "Secondary Lacuna Canal Alignment ($\%$)"

prGroups = {
    "B6 lit/+ female": 'ISPLIT(SAMPLE_AIM_NAME,"_",3) IN (69,70,71,72,73,74,107,108,109,110,111,112,113,114,115)',
    "B6 lit/+ male": 'ISPLIT(SAMPLE_AIM_NAME,"_",3) IN (75,76,77,79,80,81,82,137,138,139,140,141)',
    "B6 lit/lit female": 'ISPLIT(SAMPLE_AIM_NAME,"_",3) IN (1,2,3,4,5,6,7,8,9,102,103,104,105,106)',
    "B6xC3.B6F1 lit/lit female": 'ISPLIT(SAMPLE_AIM_NAME,"_",3) IN (18,19,20,21,22,23,24,25,26,27,28)',
    "C3.B6 lit/lit female": 'ISPLIT(SAMPLE_AIM_NAME,"_",3) IN (34,35,36,37,38,39,40,41,42,43,44,45,116,117,118,119,120,121)',
    "B6 lit/lit male": 'ISPLIT(SAMPLE_AIM_NAME,"_",3) IN (10,11,12,13,14,15,16,17,130,131,132,133,134,135,136)',
    "C3.B6 lit/lit male": 'ISPLIT(SAMPLE_AIM_NAME,"_",3) IN (46,47,48,49,50,51,52,99,145,146,147,148,149,150,151,152)',
    "B6xC3.B6F1 lit/lit male": 'ISPLIT(SAMPLE_AIM_NAME,"_",3) IN (29,30,31,32,33)',
    "C3.B6 lit/+ male": 'ISPLIT(SAMPLE_AIM_NAME,"_",3) IN (61,62,63,64,65,66,67,68,153,154,155,156,157,158,159)',
    "C3.B6 lit/+ female": 'ISPLIT(SAMPLE_AIM_NAME,"_",3) IN (53,54,55,56,57,58,59,60,122,123,124,125,126,127,128)',
}
progGrpDict = dict(
    reduce(
        lambda x, y: x + y,
        [
            [(cDex, cKey) for cDex in eval(cVal.split(" IN ")[-1])]
            for (cKey, cVal) in prGroups.items()
        ],
    )
)


def getSampleGroup(csNumber):
    sNumber = int(csNumber)
    if sNumber >= 10000:
        return "B6xC3.B6F2 +/+"
    if sNumber <= max(progGrpDict.keys()):
        if progGrpDict.has_key(sNumber):
            return progGrpDict[sNumber]
        else:
            print "Error: Sample " + str(sNumber) + " is not in database!!"
            return "INVALID"
    else:
        return "B6xC3.B6F2 lit/lit"


qkName = {}
qkName["Anterior"] = 'SAMPLE_AIM_NUMBER LIKE "%A%"'
qkName["Posterior"] = 'SAMPLE_AIM_NUMBER LIKE "%P%"'
qkName["Wild-type"] = 'SAMPLE_AIM_NUMBER LIKE "%WT%"'
qkName["Knock-out"] = 'SAMPLE_AIM_NUMBER LIKE "%KO%"'

badSampleQuery = "DENSITY<0 OR DENSITY/1000>10000000 OR (NOT (PCA1_X BETWEEN -1 AND 1)) OR (NOT (PCA1_Y BETWEEN -1 AND 1)) OR (NOT (PCA1_Z BETWEEN -1 AND 1))"


def showBadSamples(cProject_Number=None):
    if cProject is None:
        cProject_Number = projectTitle
    oList = cur.execute(
        "SELECT SAMPLE_AIM_NUMBER,COUNT(*) FROM LACUNA WHERE ("
        + badSampleQuery
        + ') AND Project_Number = "'
        + cProject
        + '" GROUP BY SAMPLE_AIM_NUMBER'
    ).fetchall()
    for cLine in oList:
        print cLine
    return [cObj[0] for cObj in oList]


def clearBadSamples(samples):
    cur.execute(
        "DELETE FROM Lacuna WHERE ("
        + badSampleQuery
        + ") AND SAMPLE_AIM_NUMBER IN ("
        + ",".join(['"' + cObj + '"' for cObj in samples])
        + ")"
    ).fetchall()
    lacDB.commit()


logParabolaFit = (
    lambda xdat, ydat: (
        nprange(log10(ydat)) / nprange(log10(xdat)),
        mean(log10(xdat)),
        max(log10(ydat)),
    ),
    lambda xdat, p: np.power(10, p[2] - p[0] * (log10(xdat) - p[1]) ** 2),
)
linearFit = (
    lambda xdat, ydat: (
        nprange(ydat) / nprange(xdat),
        mean(ydat) - nprange(ydat) / nprange(xdat) * mean(xdat),
    ),
    lambda xdat, p: p[0] * xdat + p[1],
)
cosFit = (
    lambda xdat, ydat: (
        (max(ydat) - min(ydat)) / 2,
        guessspace(xdat, ydat),
        (max(ydat) + min(ydat)) / 2,
    ),
    lambda xdat, p: p[0] * cos((xdat) * 2 * pi / p[1]) + p[2],
)

sinFit = (
    lambda xdat, ydat: (
        (max(ydat) - min(ydat)) / 2,
        guessspace(xdat, ydat),
        0,
        (max(ydat) + min(ydat)) / 2,
    ),
    lambda xdat, p: abs(p[0]) * sin((xdat - p[2]) * 2 * pi / p[1]) + p[3],
)
fSinFit = (
    lambda xdat, ydat: ((max(ydat) - min(ydat)) / 2, 0, (max(ydat) + min(ydat)) / 2),
    lambda xdat, p: abs(p[0]) * sin((xdat) * 2 * pi / 43.0 - p[2]) + p[1],
)

expDecayFit = (
    lambda xdat, ydat: (max(ydat) - min(ydat), (max(xdat)) / 5, min(ydat), min(xdat)),
    lambda xdat, p: p[0] * exp(-abs(xdat - p[3]) / abs(p[1])) + p[2],
)
simpExpDecayFit = (
    lambda xdat, ydat: (max(ydat) - min(ydat), min(ydat)),
    lambda xdat, p: p[0] * exp(-(xdat)) + p[1],
)

kexpFit = (
    lambda xdat, ydat: (
        max(ydat) - min(ydat),
        ln(abs(ydat[-1] - ydat[0])) / (xdat[-1] - xdat[0]),
        min(ydat),
    ),
    lambda xdat, p: p[0] * np.exp(p[1] * xdat) + p[2],
)


def latexFriendlyName(cField):
    cField = "".join(cField.split("&"))
    cName = ptName(cField)
    badChars = ["%", "_", "\\", "#", "[", "]", "{", "}", "~", "<", ">"]

    if cName.find("$") < 0:
        for bChar in badChars:
            cName = " ".join(cName.split(bChar))
    return cName


def ptName(cField):
    cName = cField
    if fullFieldNames.has_key(cField.upper()):
        cName = fullFieldNames[cField.upper()]
    return cName


def strcsv(x):
    return "".join([tx for tx in x if tx.isalnum()])


def strdbname(x):
    outStr = ""
    for tx in x:
        if tx.isalnum():
            outStr += tx
        else:
            outStr += " "

    outStr = "_".join(outStr.strip().upper().split(" "))
    if outStr == "GROUP":
        outStr = "MOUSE_GROUP"
    return outStr


def strd(x):
    try:
        return str(round(x, 2))
    except:
        try:
            if x == "ULTRAJAX_PR":
                return "Progenitor"
            if x.find("SOST_") > -1:

                def fbone(name):
                    return "".join(name.split("_"))

                def rbone(name):
                    return name[name.find("262_") + 4 :]

                return fbone(rbone(x))
            elif x.find("UCT_") > -1:

                def fbone(name):
                    return name[: name.find("_LACUN") + 1]

                def rbone(name):
                    return name[name.find("UCT_") + 4 :]

                return fbone(rbone(x))
            elif x.find("ULTRAJAX_") > -1:

                def rbone(name):
                    return name[name.find("ULTRAJAX_") + 9 :]

                return rbone((x))
            else:
                return str(x)
        except:
            return str(x)


from fnmatch import fnmatch

dosfilt = lambda lst, fnstr: filter(lambda x: fnmatch(x, fnstr), lst)


def SafeArray2d(lacOut):
    lacArr = np.zeros((len(lacOut), len(lacOut[0])), dtype=float)
    for ij in range(len(lacOut)):
        for ik in range(len(lacOut[0])):
            try:
                lacArr[ij, ik] = float(lacOut[ij][ik])
            except:
                print "Invalid : " + str(lacOut[ij][ik])
    return lacArr


def appendDict(inItems):
    outDict = {}
    for (key, value) in inItems:
        if outDict.has_key(key):
            outDict[key] += [value]
        else:
            outDict[key] = [value]
    return outDict


class kdict(dict):
    # Designed for On-the-fly tables from SQL DB Output
    def __getitem__(self, giArgs):
        if not self.has_key(giArgs):
            tSearch = None
            if type(giArgs) is type("bob"):
                tSearch = self._inexactMatch(giArgs)
            if tSearch is None:
                super(kdict, self).__setitem__(giArgs, kdict())
            else:
                giArgs = tSearch
        return super(kdict, self).__getitem__(giArgs)

    def __setitem__(self, *args):
        giArgs = args[0]
        if not self.has_key(giArgs):
            super(kdict, self).__setitem__(giArgs, kdict())
        super(kdict, self).__setitem__(giArgs, args[1])

    def _inexactMatch(self, giArgs):
        cMatches = dosfilt(self.keys(), giArgs)
        if len(cMatches) > 0:
            return cMatches[0]
        return None

    def __list__(self):
        print self.values()
        return [cEle.__list__() for cEle in self.values()]

    def __latex__(self):
        return self._html()

    def tup(self):
        return tuple([cEle.values() for cEle in self.values()])

    def _html(self, renderFunc=lambda x: str(x)):
        outStr = ""
        headerStr = (
            "<tr><td>Output</td><td>"
            + "</td><td>".join([str(cObj) for cObj in sorted(self.values()[0].keys())])
            + "</td></tr>"
        )
        for cName in sorted(self.keys()):
            cVal = self[cName]
            outStr += (
                "<tr><td>"
                + str(cName)
                + "</td><td>"
                + "</td><td>".join(
                    [renderFunc(self[cName][cObj]) for cObj in sorted(cVal.keys())]
                )
                + "</td></tr>"
            )
        return "<table border=1>" + headerStr + outStr + "</table>"

    def html(self, renderFunc=lambda x: str(x)):
        html(self._html(renderFunc))


class appendingkDict(dict):
    # Designed for On-the-fly tables from SQL DB Output
    def __getitem__(self, giArgs):
        if not self.has_key(giArgs):
            tSearch = None
            if type(giArgs) is type("bob"):
                tSearch = self._inexactMatch(giArgs)
            if tSearch is None:
                super(kdict, self).__setitem__(giArgs, kdict())
            else:
                giArgs = tSearch
        return super(kdict, self).__getitem__(giArgs)

    def __setitem__(self, *args):
        giArgs = args[0]
        if not self.has_key(giArgs):
            super(kdict, self).__setitem__(giArgs, kdict())
        super(kdict, self).__setitem__(giArgs, args[1])

    def _inexactMatch(self, giArgs):
        cMatches = dosfilt(self.keys(), giArgs)
        if len(cMatches) > 0:
            return cMatches[0]
        return None

    def __list__(self):
        print self.values()
        return [cEle.__list__() for cEle in self.values()]

    def __latex__(self):
        return self._html()

    def tup(self):
        return tuple([cEle.values() for cEle in self.values()])

    def _html(self, renderFunc=lambda x: str(x)):
        outStr = ""
        headerStr = (
            "<tr><td>Output</td><td>"
            + "</td><td>".join([str(cObj) for cObj in sorted(self.values()[0].keys())])
            + "</td></tr>"
        )
        for cName in sorted(self.keys()):
            cVal = self[cName]
            outStr += (
                "<tr><td>"
                + str(cName)
                + "</td><td>"
                + "</td><td>".join(
                    [renderFunc(self[cName][cObj]) for cObj in sorted(cVal.keys())]
                )
                + "</td></tr>"
            )
        return "<table border=1>" + headerStr + outStr + "</table>"

    def html(self, renderFunc=lambda x: str(x)):
        html(self._html(renderFunc))


def pTable(arr, maxWid):
    def padObj(cObj):
        if len(cObj) > maxWid - 1:
            return cObj[0 : maxWid - 1]
        else:
            return " " * (maxWid - len(cObj) - 1) + cObj

    return " ".join(["<td>" + padObj(strd(anObj)) + "</td>" for anObj in arr])


def _mitScharf(cobj, colorFunc=""):
    outVal = strd(cobj)
    if colorFunc != "":
        if colorFunc(cobj):
            outVal = '<font color="red">' + outVal + "</font>"
    return outVal


def latexTable(header, data, colorFunc=""):
    """latexTable(header,data)
    """
    outStr = r"\begin{tabular}{|" + "".join(["c|"] * len(header)) + "}"
    outStr += r"\hline"
    outStr += r" & ".join([strd(sobj) for sobj in header]) + r" \\"
    outStr += r"\hline"
    outStr += (
        r"\\".join([r" & ".join([strd(sobj) for sobj in obj]) for obj in data]) + r"\\"
    )
    outStr += r"\hline"
    outStr += r"\end{tabular}"
    return outStr


def htmlTable(header, data, colorFunc=""):
    """htmlTable(header,data)
    """
    html(
        '<font size=2><table border="1"><tr><td>'
        + "</td><td>".join([latexFriendlyName(sobj) for sobj in header])
        + "</td></tr>"
    )
    html(
        "<tr><td>"
        + "</td></tr><tr><td>".join(
            [
                "</td><td>".join([_mitScharf(sobj, colorFunc) for sobj in obj])
                for obj in data
            ]
        )
        + "</td></tr>"
    )
    html("</table></font>")


def csvTable(header, data, filename="outtable.csv"):
    outStr = ",".join([strcsv(sobj) for sobj in header]) + "\n"
    outStr += "\n".join([",".join([strd(sobj) for sobj in obj]) for obj in data]) + "\n"
    text_file = open(filename, "w")
    text_file.write(outStr)
    text_file.close()


def dbExecute(fields, table="Lacuna", wherei="", groupbyi="", sortopt="", records=-1):
    """dbExecute(fields,table='Lacuna',wherei='',groupbyi='',sortopt='',records=-1)
    # Implements Corr, Std pseudo-functions
    """
    where = ""
    if wherei != "":
        where = " WHERE " + wherei
    groupby = ""
    if groupbyi != "":
        groupby = " GROUP BY " + groupbyi

    lineByLine = False
    ksqlCmds = re.compile("(?P<cmd>\w+)?\[(?P<args>[^\]]+)?\]")
    newSubs = {}
    cbot = fields
    kDex = 0
    cSearch = ksqlCmds.search(cbot)
    while cSearch:
        kDex = 0
        cTempName = str(kDex)
        cTempName = "tempVar_" + "0" * (6 - len(cTempName)) + cTempName
        cCmd = cSearch.group("cmd")
        cArgs = cSearch.group("args")

        if cCmd == "CR":
            cArgs = cArgs.split(";")
            cArgs = ["(" + cArg + ")" for cArg in cArgs]

            if len(cArgs) > 1:
                tFields = (
                    "AVG("
                    + cArgs[0]
                    + "),AVG("
                    + cArgs[0]
                    + "*"
                    + cArgs[0]
                    + "),AVG("
                    + cArgs[1]
                    + "),AVG("
                    + cArgs[1]
                    + "*"
                    + cArgs[1]
                    + ")"
                )
                rawResult = dbExecute(
                    tFields, table, wherei, groupbyi, sortopt, records
                )
                nResult = []
                for cRes in rawResult:
                    tFields = (
                        "(AVG(("
                        + cArgs[0]
                        + "-("
                        + str(cRes[0])
                        + "))*("
                        + cArgs[1]
                        + "-("
                        + str(cRes[2])
                        + ")))"
                    )
                    cVar1 = sqrt(cRes[1] - cRes[0] * cRes[0])
                    cVar2 = sqrt(cRes[3] - cRes[2] * cRes[2])
                    tFields += "/(" + str(cVar1 * cVar2) + "))"
                    nResult += [tFields]

                if len(nResult) > 1:
                    lineByLine = len(nResult)
                    newSubs[cTempName] = nResult
                else:
                    cTempName = str(nResult[0])
        elif cCmd == "VAR":
            tFields = "AVG(" + cArgs + ")"
            rawResult = dbExecute(tFields, table, wherei, groupbyi, sortopt, records)
            nResult = []
            for cRes in rawResult:
                tFields = (
                    "AVG(("
                    + cArgs
                    + "-("
                    + str(cRes[0])
                    + "))*("
                    + cArgs
                    + "-("
                    + str(cRes[0])
                    + ")))"
                )
                # cVar1=sqrt(cRes[1]-cRes[0]*cRes[0])
                # cVar2=sqrt(cRes[3]-cRes[2]*cRes[2])
                # tFields+='/('+str(cVar1*cVar2)+'))'
                nResult += [tFields]

            if len(nResult) > 1:
                lineByLine = len(nResult)
                newSubs[cTempName] = nResult
            else:
                cTempName = str(nResult[0])
        elif cCmd == "RAT":
            tFields = "AVG(" + cArgs + ")"
            rawResult = dbExecute(tFields, table, wherei, groupbyi, sortopt, records)
            nResult = []
            for cRes in rawResult:
                tFields = (
                    "(AVG(("
                    + cArgs
                    + "-("
                    + str(cRes[0])
                    + "))*("
                    + cArgs
                    + "-("
                    + str(cRes[0])
                    + ")))"
                )
                # cVar1=sqrt(cRes[1]-cRes[0]*cRes[0])
                # cVar2=sqrt(cRes[3]-cRes[2]*cRes[2])
                tFields += "/(AVG(" + cArgs + ")*AVG(" + cArgs + ")))"
                nResult += [tFields]

            if len(nResult) > 1:
                lineByLine = len(nResult)
                newSubs[cTempName] = nResult
            else:
                cTempName = str(nResult[0])
        else:
            print "Command " + cCmd + " is not yet supported!"
            cTempName = "0"
        cbot = cbot[: cSearch.start()] + cTempName + cbot[cSearch.end() :]
        cSearch = ksqlCmds.search(cbot)

    if lineByLine > 1:
        results = []
        for curDex in range(0, lineByLine):
            smField = cbot
            for cKey in newSubs.keys():
                smField = str(newSubs[cKey][curDex]).join(smField.split(cKey))

            sqlString = (
                "select " + smField + " from " + table + " " + where + " " + groupby
            )
            print sqlString
            sqlObj = cur.execute(sqlString)
            results += [sqlObj.fetchmany(curDex + 1)[curDex]]
        return results
    else:
        sqlString = "select " + cbot + " from " + table + " " + where + " " + groupby
        sqlObj = cur.execute(sqlString)
        if records > 0:
            return sqlObj.fetchmany(records)
        else:
            return sqlObj.fetchall()


def statsTable(fields, projectName="", sqlAdd=""):
    """statsTable(fields,projectName='',sqlAdd='')
    """
    if (sqlAdd != "") & (projectName != ""):
        sqlAdd += " AND "
    if projectName != "":
        sqlAdd += ' Project_Number="' + projectName + '" '
    if sqlAdd != "":
        sqlAdd = " WHERE " + sqlAdd + " "
    genFields = []
    headFields = ["Sample Name", "Lacuna Count", "Canal Count"]
    for cField in fields:
        genFields += ["AVG(" + cField + "),AVG(" + cField + "*" + cField + ")"]
        cName = ptName(cField)
        # cName=cField
        headFields += ["Avg." + cName]
        headFields += ["Std." + cName]
    genFields += ["Sample_AIM_Number", "UNICNT(LACUNA_NUMBER)", "UNICNT(CANAL_NUMBER)"]
    nTable = (
        cur.execute(
            "select "
            + ",".join(genFields)
            + " from Lacuna "
            + sqlAdd
            + " group by SAMPLE_AIM_NUMBER"
        )
    ).fetchall()
    oData = []
    for row in nTable:

        cRow = list(row)
        canCnt = cRow.pop()
        lacCnt = cRow.pop()
        sampleName = cRow.pop()
        oRow = [sampleName, lacCnt, canCnt]
        for cEle in range(0, len(cRow) / 2):
            cMean = cRow[2 * cEle]
            cSq = cRow[2 * cEle + 1]
            oRow += [(cMean)]
            try:
                cStd = sqrt(cSq - cMean * cMean)
            except:
                cStd = -1
            oRow += [cStd]
        oData += [oRow]
    htmlTable(headFields, oData)
    csvTable(headFields, oData)


def lacunaTable(fields, projectName="", sqlAdd="", maxFetch=10000):
    """lacunaTable(fields,projectName='',sqlAdd='',maxFetch=10000):
    """
    if (sqlAdd != "") & (projectName != ""):
        sqlAdd += " AND "
    if projectName != "":
        sqlAdd += ' Project_Number="' + projectName + '" '
    if sqlAdd != "":
        sqlAdd = " WHERE " + sqlAdd + " "
    genFields = []
    headFields = ["Sample"]
    for cField in fields:
        genFields += ["" + cField + ""]
        cName = cField
        if fullFieldNames.has_key(cField.upper()):
            cName = fullFieldNames[cField.upper()]
        headFields += [cName]
    genFields += ["BoneId"]
    nTable = (
        cur.execute(
            "select "
            + ",".join(genFields)
            + " from Lacuna "
            + sqlAdd
            + " group by BoneId"
        )
    ).fetchmany(maxFetch)
    oData = []
    for row in nTable:

        cRow = list(row)
        sampleName = cRow.pop()
        oRow = [sampleName]
        for cEle in range(0, len(cRow)):
            cMean = cRow[cEle]
            oRow += [(cMean)]
        oData += [oRow]
    csvTable(headFields, oData)


def combCanalTable(
    params,
    boneName="",
    sqlAdd="",
    canalMax=999,
    minRadius=5,
    maxRadius=50,
    maskRadius=10,
    useHTML=True,
    useCSV=False,
):
    """ combCanalTable(params,boneName='',sqlAdd='',canalMax=999,minRadius=5,maxRadius=50,maskRadius=10,useHTML=True,useCSV=False)
    Canal parameters are simply normal names
    Lacuna parameters have a & before them
    L& means that are normal parameters
    AS& means they are to be printed out as avg and variance
    """
    header = []
    if boneName == "":
        sqlAddT = sqlAdd
        if len(sqlAdd) > 0:
            sqlAddT = " AND " + sqlAddT
        sqlString = (
            'select SAMPLE_AIM_NUMBER from Lacuna WHERE Project_Number="'
            + str(projectTitle)
            + '" '
            + sqlAddT
            + " group by Sample_AIM_Number"
        )
        bones = [obj[0] for obj in (cur.execute(sqlString)).fetchall()]
        nTable = []
        for cBone in bones:
            print cBone
            (header, curTable) = combCanalTable(
                params,
                cBone,
                sqlAdd,
                canalMax,
                minRadius,
                maxRadius,
                maskRadius,
                useHTML=False,
                useCSV=False,
            )
            nTable += curTable
        boneName = "summary"
    else:
        if len(sqlAdd) > 0:
            sqlAdd = " AND " + sqlAdd
        canparms = [cparm for cparm in params if cparm.upper().find("&") < 0]
        lacparms = [
            cparm.split("&")[1] for cparm in params if cparm.upper().find("L&") >= 0
        ]
        lacstats = [
            cparm.split("&")[1] for cparm in params if cparm.upper().find("AS&") >= 0
        ]
        laccounts = [
            cparm.split("&")[1] for cparm in params if cparm.upper().find("CT&") >= 0
        ]
        canparms = ["SAMPLE_AIM_NUMBER", "Canal_Number"] + canparms

        oTable = cur.execute(
            "select "
            + ",".join(canparms)
            + ' from Canal where Project_Number="'
            + str(projectTitle)
            + '" AND SAMPLE_AIM_NUMBER LIKE "'
            + boneName
            + '" group by Canal_Number'
        ).fetchmany(canalMax)

        lacparms = ["COUNT(Lacuna_Number)", "SUM(VOLUME*1000*1000*1000)"] + lacparms
        lacstatsparms = ["AVG(" + cparm + "),STD(" + cparm + ")" for cparm in lacstats]
        laczusammen = lacparms + lacstatsparms
        nTable = []
        for cRow in oTable:
            sqlString = (
                ' from Lacuna where Project_Number="'
                + str(projectTitle)
                + '" AND SAMPLE_AIM_NUMBER LIKE "'
                + boneName
                + '" AND Canal_Number='
                + str(cRow[1])
                + " AND Mask_Distance_Mean*1000>"
                + str(maskRadius)
                + " AND Canal_Distance_Mean*1000 BETWEEN "
                + str(minRadius)
                + " AND "
                + str(maxRadius)
                + " "
                + sqlAdd
            )

            lRow = cur.execute(
                "select " + ",".join(laczusammen) + sqlString
            ).fetchall()[0]

            ctRow = []
            for curCnt in laccounts:
                cval = list(
                    cur.execute(
                        "select COUNT(Lacuna_Number),SUM(VOLUME*1000*1000*1000)"
                        + sqlString
                        + " AND "
                        + curCnt
                    ).fetchall()[0]
                )
                if cval[0] is None:
                    cval[0] = 0
                if cval[1] is None:
                    cval[1] = 0
                try:
                    ctRow += [float(cval[0]) / lRow[0] * 100.0, cval[1] / lRow[1] * 100]
                except:
                    print (cval[0], lRow[0], cval[1], lRow[1])
                    ctRow += [-1, -1]

            nTable += [cRow + lRow + tuple(ctRow)]

        # Assemble the header

        for cp in canparms:
            if fullFieldNames.has_key(cp.upper()):
                header += [fullFieldNames[cp.upper()]]
            else:
                header += [cp]
        for cp in lacparms:
            if fullFieldNames.has_key(cp.upper()):
                cp = fullFieldNames[cp.upper()]
            header += ["Lac." + cp]
        for cp in lacstats:
            if fullFieldNames.has_key(cp):
                cp = fullFieldNames[cp.upper()]
            header += ["Lac.Avg." + cp]
            header += ["Lac.Std." + cp]
        for cp in laccounts:
            if fullFieldNames.has_key(cp.upper()):
                cp = fullFieldNames[cp.upper()]
            header += ["Num%" + cp]
            header += ["Vol%" + cp]

    if useHTML:
        htmlTable(header, nTable)
    elif useCSV:
        csvTable(header, nTable, strcsv(boneName) + ".csv")
    else:
        return (header, nTable)
