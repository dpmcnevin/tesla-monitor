class VehicleAsleep < Exception; end
class VehicleUnauthorized < Exception; end

class Tesla
  include HTTParty

  base_uri 'https://owner-api.teslamotors.com'
  # debug_output $stdout
  headers 'Content-type' => 'application/json',
          'Accept' => 'application/json',
          'cache-control' => 'no-cache',
          'user-agent' => 'curl/7.54.0'

  attr_accessor :access_token, :refresh_token, :vehicle_id

  def initialize
    @vehicle_id = '32600350078727683'
  end

  def vehicles; authenticated_call(method: :get, url: '/api/1/vehicles'); end
  def vehicle; authenticated_call(method: :get, url: "/api/1/vehicles/#{@vehicle_id}"); end
  def vehicle_data; authenticated_call(method: :get, url: "/api/1/vehicles/#{@vehicle_id}/data"); end

  def wake_up; authenticated_call(method: :post, url: "/api/1/vehicles/#{@vehicle_id}/wake_up"); end
  def honk_horn; authenticated_call(method: :post, url: "/api/1/vehicles/#{@vehicle_id}/command/honk_horn"); end
  def door_unlock; authenticated_call(method: :post, url: "/api/1/vehicles/#{@vehicle_id}/command/door_unlock"); end

  private

  def authenticated_call(method:, url:, retry: true)
    ## Grab a new token if we don't have one
    get_token if @access_token.nil?

    response = self.class.send(method, url, headers: {'Authorization' => "Bearer #{@access_token}"})

    ## It's a timeout, the car isn't on
    if response.code.eql?(408)
      raise VehicleAsleep
    ## Need to grab a new token and retry
    elsif response.code.eql?(401) || response.code.eql?(403)
      raise VehicleUnauthorized
    else
      return response
    end
  end

  def get_token
    response = self.class.post('/oauth/token',
                               body: {
                                 grant_type: 'password',
                                 client_id: '81527cff06843c8634fdc09e8ac0abefb46ac849f38fe1e431c2ef2106796384',
                                 client_secret: 'c7257eb71a564034f9419ee651c7d0e5f7aa6bfbd18bafb5c5c033b093bb2fa3',
                                 email: ENV['tesla_email'],
                                 password: ENV['tesla_password']
                               }.to_json)

    @access_token = response['access_token']
    @refresh_token = response['refresh_token']
  end
end
