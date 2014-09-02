from distutils.core import setup
from distutils.extension import Extension
#from Cython.Distutils import build_ext

sourcefiles = ['DBCompiled.c']

setup(
    ext_modules = [Extension("DBCompiled", sourcefiles)]
)