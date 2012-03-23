import tornado.ioloop
import tornado.web as web
import tornadio2

import random
import hashlib

USER_ID_COOKIE_NAME = "PRSESSID"
APP_PORT = 8000

def create_random_str():
	return hashlib.sha256(str(random.getrandbits(1000))).hexdigest()

class SessionManager:
	sessions = {}

	@classmethod
	def get(cls, handler):
		session_id = handler.get_cookie(USER_ID_COOKIE_NAME)
		if (not session_id) or (session_id not in cls.sessions.keys()):
			session_id = create_random_str()
			while session_id in cls.sessions.keys():
				session_id = create_random_str()
			handler.set_cookie(USER_ID_COOKIE_NAME, session_id)
			cls.sessions[session_id] = {}

		return cls.sessions[session_id]

	@classmethod
	def get_by_session_id(cls, session_id):
		return cls.sessions.get(session_id)

class WebHandler(web.RequestHandler):
	def get(self):
		#session = SessionManager.get(self)
		self.render("index.html")

class ClientHandler(WebHandler):
	def get(self):
		ua = self.request.headers["User-Agent"]
		return "iPhone" in ua or "Android" in ua or "BlackBberry" in ua or "iPad" in ua

class MyConnection(tornadio2.SocketConnection):
	connections = []

	def on_open(self, request):
		self.connections.append(request)
		print request.ip

	def on_close(self):
		print "closed!"

	@tornadio2.event("orientation")
	def orientation(self, orientation):
		print orientation
		self.emit("orientationupdate", orientation)
		

MyRouter = tornadio2.TornadioRouter(MyConnection)

routes = [
	(r"/static/(.*)", web.StaticFileHandler, {"path": "./static"}),
	(r"/images/(.*)", web.StaticFileHandler, {"path": "./images"}),
	(r"/", WebHandler),
	(r"/client", ClientHandler),
]
routes.extend(MyRouter.urls)

application = web.Application(routes, socket_io_port = APP_PORT, debug=1)

if __name__ == "__main__":
	application.listen(APP_PORT)
	tornado.ioloop.IOLoop.instance().start()
