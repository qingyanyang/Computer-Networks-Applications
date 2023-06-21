# Bonus solutions:
# (164) 1. Check the Expires header of cached objects to determine if a new copy is needed from the origin server instead of just sending back the cached copy (2 marks)
# (347) 2. Pre-fetch the associated files of the main webpage and cache them in the proxy server (DO NOT send them back to the client if the client does not request them). Look for "href=" and "src=" in the HTML. (2 marks)
# (117&249) 3. The current proxy only handles URLs of the form hostname/file. Add the ability to handle origin server ports that are specified in the URL, i.e. hostname:portnumber/file (2 marks)

# Include the libraries for socket and system calls
import socket
import sys
import os
import argparse
import re

# Library to extract and calculate age
import email.utils as eut
import datetime
from pytz import timezone

# helper function 1
# return current time based on the timeZone given
def getCurrentTime(timeZone):
  return datetime.datetime.now(timeZone).replace(tzinfo=None)

# helper function 2
# parse date to have the same format as the date returned from getCurrentTime()
def parseDate(date):
  return datetime.datetime(*eut.parsedate(date)[:6])

# 1MB buffer size
BUFFER_SIZE = 1000000

parser = argparse.ArgumentParser()
parser.add_argument('hostname', help='the IP Address Of Proxy Server')
parser.add_argument('port', help='the port number of the proxy server')
args = parser.parse_args()

# Create a server socket, bind it to a port and start listening
# The server IP is in args.hostname and the port is in args.port
# bind() accepts an integer only
# You can use int(string) to convert a string to an integer
# ~~~~ INSERT CODE ~~~~
# ~~~~ END CODE INSERT ~~~~

try:
  # Create a server socket
  # ~~~~ INSERT CODE ~~~~
  serverPort = args.port
  serverIP = args.hostname
  serverSocket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
  # ~~~~ END CODE INSERT ~~~~
  print 'Connected socket'
except:
  print 'Failed to create socket'
  sys.exit()

try:
  # Bind the server socket to a host and port
  # ~~~~ INSERT CODE ~~~~
  serverSocket.bind(('',int(serverPort)))
  # ~~~~ END CODE INSERT ~~~~
  print 'Port is bound'
except:
  print('Port is in use')
  sys.exit()

try:
  # Listen on the server socket
  # ~~~~ INSERT CODE ~~~~
  serverSocket.listen(1)
  # ~~~~ END CODE INSERT ~~~~
  print 'Listening to socket'
except:
  print 'Failed to listen'
  sys.exit()

while True:
  print '\n\nWaiting connection...'

  clientSocket = None
  try:
    # Accept connection from client and store in the clientSocket
    # ~~~~ INSERT CODE ~~~~
    clientSocket,clientAddress = serverSocket.accept()
    # ~~~~ END CODE INSERT ~~~~
    print 'Received a connection from:', args.hostname
  except:
    print 'Failed to accept connection'
    sys.exit()

  clientRequest = 'METHOD URI VERSION'
  # Get request from client
  # and store it in clientRequest
  # ~~~~ INSERT CODE ~~~~
  clientRequest= clientSocket.recv(BUFFER_SIZE)
  # ~~~~ END CODE INSERT ~~~~

  print 'Received request:'
  print '< ' + clientRequest

  # Extract the parts of the HTTP request line from the given message
  requestParts = clientRequest.split()
  method = requestParts[0]
  URI = requestParts[1]
  version = requestParts[2]

  print 'Method:\t\t' + method
  print 'URI:\t\t' + URI
  print 'Version:\t' + version
  print ''
  print ''

  # Remove http protocol from the URI
  URI = re.sub('^(/?)http(s?)://', '', URI, 1)

  # Remove parent directory changes - security
  URI = URI.replace('/..', '')

  #bonus3!!!!!!!!
  # Split hostname from resource
  resourceParts = URI.split('/', 1)
  hostnameNport = resourceParts[0]
  hostname=''
  portOfOrigin =0
  #if there is port number
  if hostnameNport.find(':')!= -1:
    hostname = hostnameNport.split(':')[0]
    portOfOrigin = int(hostnameNport.split(':')[1])
  #if there is not, use 80 by default
  else:
    hostname = hostnameNport
    portOfOrigin = 80

  resource = '/'

  if len(resourceParts) == 2:
    # Resource is absolute URI with hostname and resource
    resource = resource + resourceParts[1]

  print 'Requested Resource:\t' + resource

  cachePath = './' + hostname + resource
  if cachePath.endswith('/'):
    cachePath = cachePath + 'default'

  print 'Cache location:\t\t' + cachePath

  fileExists = os.path.isfile(cachePath)

  try:
    # Check wether the file exist in the cache
    # read the cache
    cacheFile = open(cachePath, "r")
    cacheData = cacheFile.readlines()

    print 'Cache hit! Loading from cache file: ' + cachePath

    # ProxyServer finds a cache hit
    # handle cache directives in the header field
    # Check if the cache is suitable to re-use (Any "cache-control" header in the cache?)
    # If the cache can be re-use, send back contents of cached file
    # ~~~~ INSERT CODE ~~~~
    """check max-age, see if the length of time < date of now - lastModificated
    if true, check if it is updated, send additional get request to original server
    """
    #bonus01
    #need to handle img
    responseClient=''
    with open(cachePath, "rb") as cacheFile:
      responseClient = cacheFile.read()

    timeLen = 0
    lastModificatedTime = None
    lastModificatedTimeStr = ''

    flag1 = False
    flag2 = False
    flag3 = False

    expireTime = None
    expireTimeStr = ''
    #extract info from cached data
    for content in cacheData:
      if content.startswith("Cache-Control"):
        if 'max-age' in content:
          flag1 = True
          timeLen = int(content.split('=')[-1])

      if content.startswith("Expires"):
        flag3 = True
        expireTimeStr = content.split(': ')[-1]
        expireTimeStr = expireTimeStr.strip()
        #if it is special number(e.g: 0,-1), need to send again
        if expireTimeStr=='0' or expireTimeStr=='-1':
          flag2 = True
        else:
          expireTime = parseDate(expireTimeStr)

      if content.startswith("Date"):
        lastModificatedTimeStr = content.split(': ')[-1]
        lastModificatedTime = parseDate(lastModificatedTimeStr)

    timeCurrent = getCurrentTime(timezone('UTC'))
    timeDiff = (timeCurrent - lastModificatedTime).seconds

    #check if it is expired
    #if the response doesn't have max-age, we need to check Expire header
    if not flag1:
      if not flag3:
        #if no Expires header, we need to send it to client by default
        clientSocket.sendall(responseClient)
      else:
        #if expire time is special number(e.g: 0,-1)
        if flag2:
          #if expireseconds greater than or equal to timediff, which means it is not expired
            os.remove(cachePath)
            raise IOError
        #if it is absolute date
        #current time greater than expiretime, which means it is expired
        elif timeCurrent > expireTime:
          os.remove(cachePath)
          raise IOError
        #else: it is not expired, send it to client
        else:
          clientSocket.sendall(responseClient)
    # if the response have max-age, then we don't need to check Expire header,
    # cuz the priority of max-age is higher than Expires
    else:
        if timeLen >= timeDiff:
            clientSocket.sendall(responseClient)
        else:
            #need to remove the expired file
            os.remove(cachePath)
            raise IOError
    # ~~~~ END CODE INSERT ~~~~
    cacheFile.close()
    print 'cache file closed'

  # Error handling for file not found in cache and cache is not suitable to send
  except IOError:
    originServerSocket = None
    # Create a socket to connect to origin server
    # and store in originServerSocket
    # ~~~~ INSERT CODE ~~~~
    originServerSocket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    # ~~~~ END CODE INSERT ~~~~

    print 'Connecting to:\t\t' + hostname + '\n'
    try:
      # Get the IP address for a hostname (origin server)
      hostAddress = socket.gethostbyname(hostname)

      #bonus3!!!!!!!!
      # Connect to the origin server
      # ~~~~ INSERT CODE ~~~~
      originServerSocket.connect((hostAddress,portOfOrigin))
      # ~~~~ END CODE INSERT ~~~~

      print 'Connected to origin Server'

      # Create a file object associated with this socket
      # This lets us use file function calls
      originServerFileObj = originServerSocket.makefile('+', 0)

      # Create origin server request line and headers to send
      # and store in originServerRequestHeader and originServerRequestLine
      originServerRequestLine = ''
      originServerRequestHeader = ''

      # originServerRequestLine is the first line in the request and
      # originServerRequestHeader is the second line in the request
      # ~~~~ INSERT CODE ~~~~
      originServerRequestLine = method + ' ' +resource +' '+version
      originServerRequestHeader = 'Host: ' + hostname
      # ~~~~ END CODE INSERT ~~~~

      # Construct the request to send to the origin server
      originServerRequest = originServerRequestLine + '\r\n' + originServerRequestHeader + '\r\n\r\n'

      # Request the web resource from origin server
      print 'Forwarding request to origin server:'
      for line in originServerRequest.split('\r\n'):
        print '> ' + line

      try:
        originServerSocket.sendall(originServerRequest)
      except socket.error:
        print 'Send failed'
        sys.exit()

      print 'Request sent to origin server\n'
      originServerFileObj.write(originServerRequest)

      # use to store response from the origin server
      data  = ''

      # Get the response from the origin server
      # ~~~~ INSERT CODE ~~~~
      data= originServerSocket.recv(BUFFER_SIZE)
      # ~~~~ END CODE INSERT ~~~~

      # use to determine if this response should be cached?
      isCache = True

      # Get the response code from the response
      dataLines = data.split('\r\n')
      responseCode = dataLines[0]

      # Decide which content should be cached
      # ~~~~ INSERT CODE ~~~~
      responseCode = responseCode.split()
      dataSend = ''
      # if the response status is 200, send to client and cache
      if responseCode[1] == '200':
        dataSend = data
      #if the response status is 404, send to client, no need to be cached, cannot sure if it is temporary or permanent
      if responseCode[1] == '404':
        dataSend = data
        isCache = False
      # if the repsonse status is 301, send to client and cache to response sequence of requests
      if responseCode[1] == '301':
        dataSend = data
      # if the repsonse status is 302, send to client and no need to cache, cuz it is temporaryly removed
      if responseCode[1] == '302':
        dataSend = data
        isCache = False
      # ~~~~ END CODE INSERT ~~~~

      # Send the data to the client
      # ~~~~ INSERT CODE ~~~~
      clientSocket.sendall(dataSend)
      # ~~~~ END CODE INSERT ~~~~

      # cache the content if it should be cached
      if isCache:
        # Create a new file in the cache for the requested file.
        # Also send the response in the buffer to client socket
        # and the corresponding file in the cache
        cacheDir, file = os.path.split(cachePath)
        print 'cached directory ' + cacheDir
        if not os.path.exists(cacheDir):
          os.makedirs(cacheDir)
        cacheFile = open(cachePath, 'wb')

        # Save origin server response (data) in the cache file
        # ~~~~ INSERT CODE ~~~~
        cacheFile.write(dataSend)

        #bonus2:
        indexH = 0
        indexS = 0
        l = len(dataSend)
        while indexS != -1 or indexH != -1:
            indexS = dataSend.find('src',indexS,l)
            if indexS != -1:
                print("found src!!!!!!!")
                start = dataSend.find('"',indexS,l)
                end = dataSend.find('"',start+1,l)
                indexS = end+1
                #get content of src
                url = dataSend[start+1:end]
                print(url)
                hostnameURL = ''
                #with start with http(s),need to extract hostname and resource
                if url.startswith('http://') or url.startswith('https://'):
                    startHostname = url.find('/')+2
                    endHostname = url.find('/',startHostname,len(url))
                    hostnameURL = url[startHostname:endHostname]
                    resource = url[endHostname:]
                else:
                    #just get resource,and keep the origin hostname
                    resource = '/' + url
                    hostnameURL = hostname
                originServerRequestLine = method + ' ' +resource +' '+version
                originServerRequestHeader = 'Host: '+ hostnameURL
                # Construct the request to send to the origin server
                originServerRequest = originServerRequestLine + '\r\n' + originServerRequestHeader + '\r\n\r\n'
                #send request to origin server to get the html, and cache it
                try:
                    originServerSocket.sendall(originServerRequest)
                except socket.error:
                    print ('Send failed')
                    sys.exit()
                # use to store response from the origin server
                data  = ''
                # Get the response from the origin server
                data = originServerSocket.recv(BUFFER_SIZE)
                # use to determine if this response should be cached?
                isCache = True
                # Get the response code from the response
                dataLines = data.split('\r\n')
                responseCode = dataLines[0]
                # Decide which content should be cached
                responseCode = responseCode.split()
                #if the response status is 404, send to client, no need to be cached, cannot sure if it is temporary or permanent
                if responseCode[1] == '404' or responseCode[1] == '302':
                    isCache = False
                if isCache:
                    # Create a new file in the cache for the requested file.
                    # Also send the response in the buffer to client socket
                    # and the corresponding file in the cache
                    cachePath = './' + hostnameURL + resource
                    if cachePath.endswith('/'):
                        cachePath = cachePath + 'default'
                    cacheDir, file = os.path.split(cachePath)
                    print ('cached directory ' + cacheDir)
                    if not os.path.exists(cacheDir):
                        os.makedirs(cacheDir)
                    cacheFile = open(cachePath, 'wb')
                    # Save origin server response (data) in the cache file
                    cacheFile.write(data)
                    cacheFile.close()
            #same with src
            indexH = dataSend.find('href',indexH,l)
            if indexH != -1:
                print("found href!!!!!!!")
                start = dataSend.find('"',indexH,l)
                end = dataSend.find('"',start+1,l)
                indexH = end+1
                url = dataSend[start+1:end]
                print(url)
                hostnameURL = ''
                if url.startswith('http://') or url.startswith('https://'):
                    startHostname = url.find('/')+2
                    endHostname = url.find('/',startHostname,len(url))
                    hostnameURL = url[startHostname:endHostname]
                    resource = url[endHostname:]
                else:
                    resource = '/' + url
                    hostnameURL = hostname
                originServerRequestLine = method + ' ' +resource +' '+version
                originServerRequestHeader = 'Host: '+hostnameURL
                # Construct the request to send to the origin server
                originServerRequest = originServerRequestLine + '\r\n' + originServerRequestHeader + '\r\n\r\n'
                #send request to origin server to get the html, and cache it
                try:
                    originServerSocket.sendall(originServerRequest)
                except socket.error:
                    print ('Send failed')
                    sys.exit()
                # use to store response from the origin server
                data  = ''
                # Get the response from the origin server
                data = originServerSocket.recv(BUFFER_SIZE)
                # use to determine if this response should be cached?
                isCache = True
                # Get the response code from the response
                dataLines = data.split('\r\n')
                responseCode = dataLines[0]
                # Decide which content should be cached
                responseCode = responseCode.split()
                #if the response status is 404, send to client, no need to be cached, cannot sure if it is temporary or permanent
                if responseCode[1] == '404' or responseCode[1] == '302':
                    isCache = False
                if isCache:
                    # Create a new file in the cache for the requested file.
                    # Also send the response in the buffer to client socket
                    # and the corresponding file in the cache
                    cachePath = './' + hostnameURL + resource
                    if cachePath.endswith('/'):
                        cachePath = cachePath + 'default'
                    cacheDir, file = os.path.split(cachePath)
                    print ('cached directory ' + cacheDir)
                    if not os.path.exists(cacheDir):
                        os.makedirs(cacheDir)
                    cacheFile = open(cachePath, 'wb')
                    # Save origin server response (data) in the cache file
                    cacheFile.write(data)
                    cacheFile.close()
        # ~~~~ END CODE INSERT ~~~~

        cacheFile.close()
        print 'cache file closed'

      # finished sending to origin server - shutdown socket writes
      originServerSocket.shutdown(socket.SHUT_WR)

      print 'origin server done sending'
      originServerSocket.close()

      clientSocket.shutdown(socket.SHUT_WR)
      print 'client socket shutdown for writing'
    except IOError, (value, message):
      print 'origin server request failed. ' + message
  try:
    clientSocket.close()
  except:
    print 'Failed to close client socket'