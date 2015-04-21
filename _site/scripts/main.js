$(document).ready(function() {
  $('.carousel').slick({
    dots: false,
    placeholders: true,
    infinite: true,
    arrows: false,
    speed: 1500,
    autoplay: true,
    autplaySpeed: 2500,
    slidesToShow: 4,
    slidesToScroll: 4,
    responsive: [
      {
        breakpoint: 1400,
        settings: {
          arrows: false,
          slidesToShow: 3,
          slidesToScroll: 1
        }
      },
      {
        breakpoint: 1000,
        settings: {
          arrows: false,
          slidesToShow: 2,
          slidesToScroll: 1
        }
      },
      {
        breakpoint: 700,
        settings: {
          arrows: false,
          slidesToShow: 1,
          slidesToScroll: 1
        }
      }
    ]
  });
});
