require 'bundler/setup'
Bundler.require

require_relative 'tesla'

$stdout.sync = true

class Application

  def self.send_data(metric:, value:, metric_type: 'g')
    u = UDPSocket.new()

    statsd_string = "tesla.#{metric}:#{value}|#{metric_type}"

    u.send(
      statsd_string,
      0, 'graphite', 8125
    )

    u.close
  end

  def self.collapse_hash(hash:, prefix: nil)
    collapsed_hash = {}

    hash.each do |key, val|
      if val.is_a?(Hash)
        collapsed_hash.merge!(
          collapse_hash(hash: val, prefix: [prefix, key].compact.join('.'))
        )
      elsif val.kind_of?(Numeric)
        collapsed_hash[[prefix, key].compact.join('.')] = val
      end
    end

    return collapsed_hash
  end

end

puts 'Starting Application'

$tesla_client = Tesla.new
sleep_duration = 30

while(true)
  begin
    print "#{Time.now} :: Getting Data.. "
    data = $tesla_client.vehicle_data

    Application.collapse_hash(hash: data).each do |key, val|
      Application.send_data(metric: key, value: val)
    end

    ## Reset the sleep duration
    puts 'done'
    sleep_duration = 30
  rescue VehicleAsleep => e
    sleep_duration = [180, sleep_duration+30].min
    puts "Vehicle asleep, skipping, sleeping for #{sleep_duration}"
  end

  sleep(sleep_duration)
end
