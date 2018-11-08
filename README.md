
Problem statement:

Build a service which gets a long URL and returns shortened representation called short url

Extra features:
Create a tracking mechanism to track the number of “clicks” on a particular short URL

Requirements:
The unique URL should always be shortened to the same unique short URL
Short url size should be reasonable small to reduce memory consumption

Considerations: service should address scalability and durability

Service design (core concepts):
API - how user will interact with a service
Application layer
Persistence layer
Encoding algorithm

API
A client talks to the service via REST api, thus we need to have at least two endpoints:
POST where body contains a Long URL
Response codes:
200 - returns short url
400 - bad request if the given long url is not valid url

GET where shortUrl is a path parameter
Response codes:
200 - returns long url
404 - not found if there is no long url associated with the given short url

Architecture overview and design:
Main requirements: system should be fast, scalable and durable.

To achieve a better performance we can use load balancer (front-end for the service) to spread client requests across multiple workers.
Load balancer should implement a back pressure mechanism and handle some cases when some workers might fail due to outages, slow or faulty
workers, starvation and etc. In order to improve performance on a read operations we can use a cache such as Memcached, Redis and etc.
Why we’d like to use cache ? Once a user has created a short url he will post it on a social media which means that short url is going to
be used intensively in the next few hours that’s why it makes sense to use some caching solution.
Worker in our design serves for consuming user’s requests sent from a load balancer, generating short url and storing it into a
persistent storage.
Please follow the link below to view the block diagram:
https://drive.google.com/open?id=1dCEk7jbOEkdHhox7rBr8biNy92sRzuJC

Workflow:

Client sends a http request to a load balancer using REST endpoints mentioned previously
Load balancer redirects a request to some worker
Worker asks the cache to check if there is already a short url associated with the given long url
3.1 if there is a short url in cache then just return it (also increment click counter, I will address it later in the doc)
3.2 if there is no short url in cache then generate it and persist into a database, return result to the client

Encoding algorithm:
Requirements:
The size of a short url should not exceed 7 characters (my assumption since it’s not specified in the coding assessment)
For each unique long url should be generated a unique short url. For any two equivalent long URLs we should ideally generate
equivalent short URLs (collisions are possible, will address this later in the doc). This will allow us to reduce memory (storage) consumption

Analysis:
What kind of character we can use to generate a unique url? We can use alphabet characters and numbers, i.e.:
[a - z] -  26 characters
[A - Z] - 26 characters
[0 - 9] - 10 characters

62 characters in total. If a short url is 7 character long we can calculate how many possible unique values we can have:
62^7 = 3500000000000  combinations. Having said that we can figure out how fast we will exhaust all possible values based on number
of incoming requests per second:
10 thousands request per sec = 3.5 trillion divided by 10 thousand = 350 000 000 which is around 11 years
In case of a  million of requests pers sec = around 40 days
Having said that than more requests you need to process than more characters you should use for the short url


Memory analysis
Any number from 0 to 3.5 trillion can we represented 42 bits, prove:
3500000000000 (base 10) or 110010111011101000010000011011100000000000 (base 2 and 42 bits).
Java code to get base 2 representation: Long.toBinaryString(3500000000000L)

Possible techniques:

Using counter (rejected)
This technique is based on an integer number that gets incremented by 1 per each encoding method invocation.
Issues associated with this approach are the following:
the range of url is limited by the range of int;
integer overflow will lead to overwriting existing urls;
two equal long urls will have different short urls;
short url can be bigger than long url and bigger than 7 characters;
easy to predict next short url, not secure.

2. Counter + base 62 (rejected)
This approach is similar to the Counter technique but instead of using just a number we need to convert it to a characters
sequence using base 62, i.e. % by 62 to get character and divide by 62, that will give us a sequence of number between 0 and 61.
This approach has the same issues as the previous one: the range of url is limited by the range of int, integer overflow,
short url oversize and etc. How can we do better ?

3. Random fixed length (good solution but doesn’t provide consistent results hence rejected)
Idea: use fixed length short url (7 in our case) and generate a random number between 0 and 61, convert that number to character
Pros:
This will give us approximately a large number of possible encodings;
No limit by the range of int;
no overflow;
Good performance;
Hard to predict.

Cons:
Collisions are possible, if that the case then length can be increased
Not consistent encoding, the algorithm generates two different short urls for the same long url which will lead to suboptimal memory usage

4. Using MD5 hashing (approved)
In order to achieve consistent encoding we can use MD5 hashing. MD5 algorithm produces 128 bit hash , however we need only 42 bits.
We can take first 42 bits convert them to decimal using base 2 and then convert the number to a sequence of characters using base 62.
Also we can use string hashCode() function provided by java, but it has a higher probability of collisions than other solutions listed here.
Pros:
Algorithm produces consistent encoding for the same long url as result reduces memory consumption
Cons:
Collisions are possible (will address below)


Collisions resolution in MD5 approach:
Since we are going to use only 42 bits we can calculate collision probability using the following python code:
K=1000000 // one million hash functions
N = 2^42 // number of different values

import math
N = pow(2, 42)
probUnique = 1.0

for k in range(1, K):
    probUnique = probUnique * (N - (k - 1)) / N


print(1.0 - probUnique)

Probability of collision for 1000000 is 10%
source : http://preshing.com/20110504/hash-collision-probabilities/

I noticed a collisions on 10000000 samples

To reduce collision probability we can use buckets and split all range of hashes across them.
Since we have 128 hash produced by MD5 we can calculate a bucket number using the following formula:

number % hash.size() + 15
15 - offset because result of modulo operation can produce numbers in [-15, 15]
Thus we need only 30 buckets to cover the whole range of hash values.
Note: we should not store extra 2 chars into a database instead before storing a key-value we cut them to determine a
bucket number and store only 7 length hash string, but we do send 8-9 length hash to the client.

After this improvement no collisions were noticed. Uniform distribution of hashes is good.


Persistence storage
For simplicity's sake in this assessment I used a HashMap, however it’s not a good solution since it’s not scalable nor durable.
In production I’d rather use some SQL or no-SQL database

Concurrency:
In this assessment I used Akka framework. Since intrinsically Actor model is a synchronizer, meaning it can be used as a
synchronization primitive and since it’s Actor is Thread safe we don’t need extra synchronization  logic.
However, in this assessment I used single instance of Actor per multiple request, thus if we use Actor per request then
we need to reconsider our design. For instance we can use bucket level locking, document level locking in document
database or some transaction mechanism (ACID)



HOW TO run and use

From the project directory: sbt run
Send POST request to localhost:8080 with row body, just a long url
Example curl: curl -X POST -d https://github.com/dmgcodevil localhost:8080
Response: 127.0.0.1:8080/umWvN3Q4

Copy paste short url into your browser, you should get a long url
In order to get clicks count for the short url type: http://127.0.0.1:8080/api/stats/clicks/umWvN3Q4