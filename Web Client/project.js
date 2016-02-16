$(document).ready(function(){
	
		var dD = $("#dropdown");
		var pH = $("#placeHeading");
		var tL = $("#trendList");
		var tH = $("#trendHeading");
		var pL = $("#pageList");
		var wF = $("#wikiFrame");
		var eD = $("#editsDiv");
		var tD = $("#tweetsDiv");
		var twH = $("#tweetsHeader");
		var twL = $("#tweetsList");
		var dDdom = dD.get(0);
		var trendList = [];
		var pageList = [];
		var tweetList = [];
		var cTrend;
		var cPage;
		var cState = "Wikipedia";
		eD.hide();
		tD.hide();
		
		
		
		
		var updateTrends = function(){
			$.get("TwikfeedServlet?Type=Trends").done(function(data, textStatus) {
			
			alert(data);
		
			trendList = $.parseJSON(data);
			pH.empty();
			tL.empty();
			pH.html(dDdom.options[dDdom.selectedIndex].value + " Trends");
			
			
			
			
		
		
			var i;
			for(i=0; i < trendList.length; i++){
				tL.append("<li>" + trendList[i].name + "</li>");
			}
			}, "text");
			
		}
		
		var updatePages = function(event){
			cTrend = $(event.target);
			if (cTrend[0].nodeName == "LI"){
				
				//Now make a get request to get the list of page name
				var trendIndex = cTrend.index();
				$.get("TwikfeedServlet?Type=Articles&id="+trendList[trendIndex].id).done(function(data, textStatus) {
					alert(data);
		
					pageList = $.parseJSON(data);
					pL.empty();
					tH.html(cTrend.html()+ "<br>related Wikipedia pages");
			
				
					var i;
					for(i=0; i < pageList.length; i++){
						pL.append("<li>" + pageList[i].title + "</li>");
					}
				}, "text");
				$.get("TwikfeedServlet?Type=Tweets&id="+trendList[trendIndex].id).done(function(data, textStatus) {
					alert(data);
		
					tweetList = $.parseJSON(data);
					twH.html(trendList[trendIndex].name + "Tweets");
					twL.empty();
					var i;
					for(i=0; i < tweetList.length; i++){
						twL.append("<li>" + tweetList[i].content + "<br>" + tweetList[i].time + "</li>");
					}
					
					
				}, "text");
			}
		}
		
		var updatePage = function(event){
			cPage = $(event.target);
			
			if (cPage[0].nodeName == "LI"){
				var pageIndex = cPage.index();
				alert(pageIndex);
			
				wF.get(0).src = pageList[pageIndex].url;
				
			}
		}
		
		
			
		updateTrends();
		dD.change(updateTrends);
		(tL.get(0)).onclick = updatePages;	
		(pL.get(0)).onclick = updatePage;
		
		$("#bTweets").click(function(){
			if(cState != "Tweets"){
				wF.hide();
				eD.hide();
				tD.show();
				cState = "Tweets";
			}
		});
		$("#bEdits").click(function(){
			if(cState != "Edits"){
				wF.hide();
				eD.show();
				tD.hide();
				cState = "Edits";
			}
		});
		$("#bWiki").click(function(){
			if(cState != "Wikipedia"){
				wF.show();
				eD.hide();
				tD.hide();
				cState = "Wikipedia";
			}
		});
	});
		
		
	