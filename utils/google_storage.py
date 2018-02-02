from __future__ import print_function, division
import sys
import os
sys.path.append(os.path.abspath("."))
sys.dont_write_bytecode = True

__author__ = "bigfatnoob"

from utils import lib
from google.cloud import storage
from utils.cache import mkdir, file_exists
from joblib import Parallel, delayed
import logging
from utils.logger import get_logger

LOG_LEVEL = logging.INFO
logger = get_logger(__name__, LOG_LEVEL)



BUCKET_NAME = "enableviz-1380.appspot.com"
BUCKET = None


def get_bucket():
  """
  Singleton to return bucket.
  :return:
  """
  global BUCKET
  if BUCKET is None:
    BUCKET = storage.Client().get_bucket(BUCKET_NAME)
  return BUCKET


def implicit():
  """
  Check if client is validly configured
  """
  storage_client = storage.Client()
  # Make an authenticated API request
  buckets = list(storage_client.list_buckets())
  print(buckets)


def get_blob(name):
  return get_bucket().blob(name)


def download_blob(name, download_path):
  """
  Download a blob from Google Cloud Storage
  :param name: Name of Blob
  :param download_path: Path to be saved
  """
  blob = get_blob(name)
  name = blob.name.rsplit("/", 1)[-1]
  mkdir(download_path)
  if download_path[-1] == "/":
    destination_file = "%s%s" % (download_path, name)
  else:
    destination_file = "%s/%s" % (download_path, name)
  if file_exists(destination_file):
    print("%s already exists" % destination_file)
    return name
  else:
    blob.download_to_filename(destination_file)
    print('Blob {} downloaded to {}.'.format(name, destination_file))
  return destination_file


def download_blobs(prefix_path, download_path, n_jobs, start=0, max_results=None, do_parallel=True):
  """
  :param prefix_path:
  :param download_path:
  :param n_jobs:
  :param max_results:
  :param do_parallel
  :return:
  """
  mkdir(download_path)
  blobs = []
  i = -1
  for stat in get_bucket().list_blobs(prefix=prefix_path):
    i += 1
    if i < start or stat.size == 0:
      continue
    blobs.append(stat.name)
    if max_results is not None and len(blobs) >= max_results:
      break
  print("# Files =", len(blobs))
  if do_parallel:
    Parallel(n_jobs=n_jobs)(delayed(download_blob)(name, download_path) for name in blobs)
  else:
    for name in blobs:
      download_blob(name, download_path)


def list_blobs(prefix_path, max_results=None):
  """
  List all blobs in folder in bucket
  :param prefix_path: Path of the folder
  :param max_results: Max Results to be listed. If None, return all
  :return: List of strings
  """
  blobs = []
  for blob in get_bucket().list_blobs(prefix=prefix_path):
    if blob.size == 0:
      continue
    blobs.append(blob.name)
    if max_results is not None and len(blobs) >= max_results:
      break
  return blobs


def _download_blobs(source, destination, start=0, max_results=None):
  """
  :param source:
  :param destination:
  :param max_results:
  :return:
  """
  n_jobs = 1
  # source, max_results = "pyfiles/csv/", 2
  # source, max_results = "pyfiles/csv_all/", None
  do_parallel = False
  args = sys.argv
  if len(args) >= 2 and lib.is_int(args[1]):
    n_jobs = int(args[1])
    do_parallel = True
  print("Running as %d jobs" % n_jobs)
  download_blobs(source, destination, n_jobs, start, max_results, do_parallel=do_parallel)

if __name__ == "__main__":
  # implicit()
  _download_blobs("cfiles/csv_all", "data/cfiles_dump/csv_all", start=1, max_results=100)

