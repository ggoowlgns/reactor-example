### API Components
* Publisher, Subscriber, Subscription

Reactive Streams 특징
1. asynchronous
2. non-blocking
3. Backpressure

### Data Process
1. Publisher 에 Subscriber 등록 : Publisher.subscribe(Subscriber)
2. 1.이 정상적으로 이루어지면 Subscription 이 생성된다.
3. Publisher 가 Subscriber.onSubscribe(Subscription) 호출
4. Subscriber 은 Subscription.request(numOfElements) 호출 : back-pressure
5. Publisher는 susbscirber 의 onNext 를 호출한다.
   1. until :
      1. Publisher sends all elements requested : publisher 가 10개의 cake 을 보낼수 있는데, subscriber 는 5개만 받을수 있다. -> subscriber 는 subscription 에게 나는 5개만 받을 수 있다고 전달. -> Publisher 는 5개만 준다. -> 후에는 Subscription 은 cancel 될 수도 있고, 추가로 받을 수도 있다.
      2. Publisher sends all elements it has. (onComplete) -> subscription, subscriber 은 canceled
      3. Error 발생 -> (onError) -> subscription, subscriber 은 canceled
             
